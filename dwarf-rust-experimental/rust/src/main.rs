// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

mod apperror;
mod wasm;

use apperror::Error;
use fallible_iterator::FallibleIterator;
use gimli::{constants, Dwarf, EndianSlice, LineRow, LittleEndian, Reader};
use indexmap::IndexMap;
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap};
use std::convert::TryInto;
use std::io::Read;
use wasm::{parse_sections, SectionKind};

pub struct Pos {
  line: u32,
  // column: u32,
}

// enum FuncState {
//   Start,
//   Ignored,
//   Normal,
// }

#[derive(Default, Debug, Clone)]
pub struct Line {
  id: u64,
  file_id: u32,
  address: u64,
  line: u32,
  // column: u32,
  score: u32,
}

#[derive(Default, Debug, Clone)]
pub struct ScoredSourceFile {
  id: u32,
  directory: Option<String>,
  file: String,
  language: u16,
  lines: Vec<Line>,
}

#[derive(Default, Debug, Clone)]
pub struct ScoredSourceUnit {
  name: String,
  directory: String,
  files: Vec<ScoredSourceFile>,
}

#[derive(Default, Serialize, Deserialize, PartialEq, Debug, Clone)]
pub struct SourceFile {
  id: u32,
  #[serde(skip_serializing_if = "Option::is_none")]
  directory: Option<String>,
  file: String,
  language: u16,
}

#[derive(Default, Serialize, Deserialize, PartialEq, Debug)]
pub struct SourceUnit {
  name: String,
  directory: String,
  files: Vec<SourceFile>,
}

#[derive(Default, Serialize, Deserialize, PartialEq, Debug)]
pub struct SourceResult {
  #[serde(skip_serializing_if = "Option::is_none")]
  units: Option<Vec<SourceUnit>>,
  lines: Option<Vec<Vec<u64>>>,

  #[serde(skip_serializing_if = "Option::is_none")]
  functions: Option<BTreeMap<String, Vec<u64>>>,

  #[serde(skip_serializing_if = "Option::is_none")]
  error: Option<String>,
}

pub fn extract_source_info<R: Reader + Clone + Default>(src: R) -> Result<SourceResult, Error> {
  let mut sections = HashMap::new();

  for section in parse_sections(src.clone())?.iterator() {
    let section = section?;

    match section.kind {
      SectionKind::Custom { name } => {
        if name.starts_with(".debug_") {
          sections.insert(name, section.payload);
        }
      }
      _ => {}
    }
  }

  let dwarf = Dwarf::load::<_, _, Error>(
    |id| Ok(sections.get(id.name()).cloned().unwrap_or_default()),
    |_| Ok(Default::default()),
  )?;

  let mut scored_source_units = vec![];

  let mut iter = dwarf.units();
  let mut best_scores: HashMap<u64, Line> = HashMap::new();
  let mut next_entry_id = 0u64;
  let mut next_file_id = 0u32;
  let mut functions: BTreeMap<String, Vec<u64>> = BTreeMap::new();

  while let Some(unit) = iter.next()? {
    let mut unit = dwarf.unit(unit)?;

    let line_program = match unit.line_program.take() {
      Some(line_program) => line_program,
      None => continue,
    };

    let lang = {
      let mut entries = unit.entries();
      entries.next_entry()?;
      match entries
        .current()
        .unwrap()
        .attr_value(gimli::DW_AT_language)?
      {
        Some(gimli::AttributeValue::Language(lang)) => lang.0,
        _ => 0,
      }
    };

    // Extract function information from DWARF entries
    {
      let mut entries = unit.entries();
      while let Some((_depth, entry)) = entries.next_dfs()? {
        if entry.tag() == gimli::DW_TAG_subprogram {
          // Get function name
          let func_name = if let Some(attr) = entry.attr_value(gimli::DW_AT_name)? {
            match attr {
              gimli::AttributeValue::DebugStrRef(offset) => {
                match dwarf.debug_str.get_str(offset) {
                  Ok(name_str) => Some(name_str.to_string()?.to_string()),
                  Err(_) => None,
                }
              }
              gimli::AttributeValue::String(name_str) => {
                Some(name_str.to_string()?.to_string())
              }
              _ => None,
            }
          } else {
            None
          };
          

          // Get low_pc (start address) and high_pc (end address or size)
          let low_pc = if let Some(attr) = entry.attr_value(gimli::DW_AT_low_pc)? {
            match attr {
              gimli::AttributeValue::Addr(addr) => Some(addr),
              _ => None,
            }
          } else {
            None
          };

          let high_pc = if let Some(attr) = entry.attr_value(gimli::DW_AT_high_pc)? {
            match attr {
              gimli::AttributeValue::Addr(addr) => Some(addr),
              gimli::AttributeValue::Udata(offset) => {
                // high_pc can be an offset from low_pc
                if let Some(low) = low_pc {
                  Some(low + offset)
                } else {
                  None
                }
              }
              _ => None,
            }
          } else {
            None
          };

          // If we have all the information, add it to our functions map
          if let (Some(func_name), Some(start), Some(end)) = (func_name, low_pc, high_pc) {
            // Filter out synthetic/relocated addresses similar to line processing
            const MAX_REALISTIC_WASM_ADDR: u64 = 0x40000000; // 1GB threshold
            if start <= MAX_REALISTIC_WASM_ADDR && end <= MAX_REALISTIC_WASM_ADDR && start < end {
              functions.insert(func_name, vec![start, end]);
            }
          }
        }
      }
    }

    let mut scored_source_files: IndexMap<(Option<String>, String), ScoredSourceFile> =
      IndexMap::new();

    let mut rows = line_program.rows();

    // let mut func_state = FuncState::Start;

    while let Some((header, row)) = rows.next_row()? {
      // if let FuncState::Start = func_state {
      //   func_state = if row.address() == 0 {
      //     FuncState::Ignored
      //   } else {
      //     FuncState::Normal
      //   };
      // }
      // if let FuncState::Ignored = func_state {
      //   if row.end_sequence() {
      //     func_state = FuncState::Start;
      //   }
      //   continue;
      // }
      // if row.end_sequence() {
      //   func_state = FuncState::Start;
      // }

      let file = match row.file(header) {
        Some(file) => file,
        None => continue,
      };

      let pos = {
        let mut line = match row.line() {
          Some(line) => line.checked_sub(1).unwrap().try_into().unwrap(),
          None => continue, // couldn't attribute instruction to any line
        };

        // It seems we need to add 1 to the line numbers for Rust.
        let is_rust = lang == constants::DW_LANG_Rust.0;
        if is_rust {
          line += 1;
        }

        Pos { line }
      };

      let addr: u64 = row.address().try_into().unwrap();

      // Filter out synthetic/relocated addresses that are too high to be realistic WASM code offsets
      // Most WASM modules are much smaller than 1GB, so addresses > 0x40000000 are likely synthetic
      const MAX_REALISTIC_WASM_ADDR: u64 = 0x40000000; // 1GB threshold
      if addr > MAX_REALISTIC_WASM_ADDR {
        continue;
      }

      let directory = if let Some(dir) = file.directory(header) {
        let dir = dwarf.attr_string(&unit, dir)?;
        let dir = dir.to_string()?;
        let dir = dir.to_string();
        Some(dir)
      } else {
        None
      };

      let filename = dwarf.attr_string(&unit, file.path_name())?;
      let filename = filename.to_string()?;
      let filename = filename.to_string();

      let score = calculate_mapping_score(&directory, &filename, pos.line as u64, &row);

      let file_entries = match scored_source_files.entry((directory.clone(), filename.clone())) {
        indexmap::map::Entry::Occupied(entry) => entry.into_mut(),
        indexmap::map::Entry::Vacant(entry) => {
          let source_file = entry.insert(ScoredSourceFile {
            id: next_file_id,
            directory,
            file: filename.to_string(),
            language: lang,
            lines: Vec::new(),
          });
          next_file_id += 1;
          source_file
        }
      };

      let line = Line {
        id: next_entry_id,
        file_id: file_entries.id,
        address: addr,
        line: pos.line,
        // column: pos.column,
        score,
      };
      next_entry_id += 1;

      file_entries.lines.push(line.clone());
      if let Some(existing) = best_scores.get(&line.address) {
        if existing.score < line.score {
          best_scores.insert(line.address, line);
        }
      } else {
        best_scores.insert(line.address, line);
      }
    }

    let source_unit = ScoredSourceUnit {
      name: unit
        .name
        .as_ref()
        .map(|x| x.to_string())
        .transpose()?
        .unwrap_or_default()
        .to_string(),
      directory: unit
        .comp_dir
        .as_ref()
        .map(|x| x.to_string())
        .transpose()?
        .unwrap_or_default()
        .to_string(),
      files: scored_source_files.values().cloned().collect(),
    };
    if !source_unit.files.is_empty() {
      scored_source_units.push(source_unit);
    }
  }

  // Iterate through the source units and their files to and remove any
  // files that don't have any lines in the best_scores map.
  for unit in &mut scored_source_units {
    for file in &mut unit.files {
      file.lines.retain(|line| {
        if let Some(best_line) = best_scores.get(&line.address) {
          line.id == best_line.id
        } else {
          false
        }
      });
    }
    unit.files.retain(|file| !file.lines.is_empty());
  }
  scored_source_units.retain(|unit| !unit.files.is_empty());

  // convert the scored source units to the final format
  let res: Vec<SourceUnit> = scored_source_units
    .into_iter()
    .map(|unit| SourceUnit {
      name: unit.name,
      directory: unit.directory,
      files: unit
        .files
        .into_iter()
        .map(|file| SourceFile {
          id: file.id,
          directory: file.directory,
          file: file.file,
          language: file.language,
        })
        .collect(),
    })
    .collect();

  let mut lines: Vec<Vec<u64>> = best_scores
    .values()
    .map(|line| vec![line.address, line.file_id as u64, line.line as u64])
    .collect();
  lines.sort_by_key(|line| line[0]);

  Ok(SourceResult {
    units: Some(res),
    lines: Some(lines),
    functions: if functions.is_empty() { None } else { Some(functions) },
    error: None,
  })
}

/// Calculate a score for how "good" a source mapping is. Higher scores are better.
/// This helps determine which mapping to keep when multiple source locations map to the same address.
fn calculate_mapping_score(
  directory: &Option<String>,
  filename: &str,
  line_number: u64,
  row: &LineRow,
) -> u32 {
  let mut score = 1000u32; // Base score

  if !row.is_stmt() {
    score = score.saturating_sub(400); // Heavy penalty for non-statement locations
  }

  if row.prologue_end() || row.epilogue_begin() {
    score = score.saturating_sub(300); // Heavy penalty for prologue/epilogue code
  }

  if row.basic_block() {
    score = score.saturating_sub(200); // Penalty for basic block boundaries
  }

  // Strategy 1: Prefer user code over library/dependency code
  if let Some(dir) = directory {
    if dir.contains("/rustc/") || dir.contains("/rust/deps/") {
      score -= 300; // Heavily penalize compiler/dependency paths
    }
    if dir.contains("library/") {
      score -= 200; // Penalize standard library
    }
    if dir.starts_with("/") && !dir.contains("src") {
      score -= 100; // Penalize absolute paths that don't look like source
    }
  }

  // Strategy 2: Prefer certain file types
  if filename.ends_with(".rs") {
    score += 100; // Prefer Rust source files
  }
  if filename.contains("main.rs") || filename.contains("lib.rs") {
    score += 50; // Prefer entry point files
  }
  if filename.contains("mod.rs") && !filename.contains("intrinsics") {
    score += 30; // Prefer module files (but not intrinsics)
  }

  // Strategy 3: Avoid generated/internal files
  if filename.contains("intrinsics") || filename.contains("panic") {
    score -= 150; // Avoid compiler intrinsics and panic handlers
  }
  if filename.contains("macros.rs") {
    score -= 100; // Macro definitions are often less useful for debugging
  }
  if filename.contains("impls.rs") || filename.contains("builders.rs") {
    score -= 80; // Implementation details vs user code
  }

  // Strategy 4: Prefer specific line numbers over line 0
  if line_number == 0 {
    score -= 200; // Line 0 is often a catch-all or generated
  } else if line_number < 10 {
    score -= 50; // Very early lines might be imports/boilerplate
  }

  // Strategy 5: Prefer shorter, simpler paths (user code is typically closer to root)
  if let Some(dir) = directory {
    let path_depth = dir.matches('/').count();
    if path_depth > 5 {
      score -= (path_depth as u32 - 5) * 20; // Penalize deeply nested paths
    }
  }

  score
}

fn run() -> Result<(), Error> {
  let mut buffer = Vec::new();
  std::io::stdin()
    .read_to_end(&mut buffer)
    .map_err(Error::Io)?;
  let slice = EndianSlice::new(buffer.as_slice(), LittleEndian);
  let result = extract_source_info(slice)?;
  serde_json::to_writer(std::io::stdout(), &result).map_err(Error::Json)?;
  Ok(())
}

fn main() {
  run().unwrap_or_else(|err| {
    let error_doc = SourceResult {
      error: Some(err.to_string()),
      units: None,
      lines: None,
      functions: None,
    };
    serde_json::to_writer(std::io::stdout(), &error_doc).unwrap_or_else(|err| {
      eprintln!("Error: {}", err);
      std::process::exit(2);
    });
    std::process::exit(1);
  });
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  pub fn test_parse() {
    // read a file in
    let src = std::fs::read("./src/count_vowels.wasm").expect("could not read wasm file");
    let actual = extract_source_info(EndianSlice::new(src.as_slice(), LittleEndian))
      .expect("extract_soruce_info failed");

    // Optionally write the expected test data when REGENERATE_TEST_DATA env var is set
    if std::env::var("REGENERATE_TEST_DATA").is_ok() {
      std::fs::write(
        "./src/count_vowels.wasm.json",
        serde_json::to_string_pretty(&actual).expect("json failed"),
      )
      .unwrap();
      println!("Regenerated test data in ./src/count_vowels.wasm.json");
    }

    // read count_vowels.rs.wasm.json
    let expected: SourceResult = serde_json::from_reader(
      std::fs::File::open("./src/count_vowels.wasm.json").expect("could not read json file"),
    )
    .expect("json failed");
    assert_eq!(actual, expected);
  }
}
