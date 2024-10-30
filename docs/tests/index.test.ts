import { describe, it, expect, beforeAll } from "@jest/globals";
import * as fs from 'fs';
import * as path from 'path';
import * as jbang from '@jbangdev/jbang';
import * as approvals from "approvals";
import {configure} from "approvals/lib/config";
import {JestReporter} from "approvals/lib/Providers/Jest/JestReporter";

const markdownFiles = [];
const includedFolders = ["docs"]

// collecting all markdown files to be tested
includedFolders.forEach(dir => {
  fs.readdirSync(path.join(__dirname, "..", dir), { recursive: true }).forEach(file => {
    if (file.toLowerCase().endsWith(".md")) {
      markdownFiles.push(path.join(dir, file))
    }
  });
});

describe("ApprovalTests", () => {
  beforeAll(() => {
    configure({
      reporters: [new JestReporter()],
    }); 
  });

  it.each(markdownFiles)('test %s', (f) => {
    const jbangExec = jbang.exec(f);
    expect(jbangExec.code).toBe(0);
    if (jbangExec.stderr.toLowerCase().includes("error")) {
        throw jbangExec.stderr;
    }

    if (!fs.existsSync(f + ".result")) {
      throw "Result file not found: " + f + ".result";
    }
    const result = fs.readFileSync(f + ".result").toString();
    approvals.verify(path.join(__dirname, "approvals"), f.replace(/\\/g, "-").replace(/\//g, "-"), result);
  });
});
