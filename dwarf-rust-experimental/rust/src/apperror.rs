use std::{error, fmt};

#[derive(Debug)]
pub enum Error {
  InvalidMagic,
  UnsupportedVersion(u32),
  MissingCodeSection,
  Reader(gimli::Error),
  InvalidPath(String),
  Io(std::io::Error),
  Json(serde_json::Error),
  Internal(&'static str),
}

impl fmt::Display for Error {
  fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
    match self {
      Error::InvalidMagic => write!(f, "WebAssembly magic mismatch."),
      Error::UnsupportedVersion(v) => {
        write!(f, "Unsupported WebAssembly version {}.", v)
      }
      Error::MissingCodeSection => write!(f, "Missing code section."),
      Error::Reader(err) => write!(f, "{}", err),
      Error::InvalidPath(err) => write!(f, "{}", err),
      Error::Io(err) => write!(f, "io error: {}", err),
      Error::Json(err) => write!(f, "json error: {}", err),
      Error::Internal(msg) => write!(f, "internal error: {}", msg),
    }
  }
}

impl error::Error for Error {
  fn source(&self) -> Option<&(dyn error::Error + 'static)> {
    match self {
      Error::Reader(err) => Some(err),
      _ => None,
    }
  }
}

impl From<gimli::Error> for Error {
  fn from(err: gimli::Error) -> Self {
    Error::Reader(err)
  }
}
