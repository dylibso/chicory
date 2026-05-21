# Chicory WebAssembly Component Model Support

This module provides WIT (WebAssembly Interface Types) and Component Model support for Chicory.

## Status

This is a work-in-progress implementation of WebAssembly Component Model support.

## Architecture

The module provides:
- **WIT Parser**: Parses WIT interface definitions
- **Canonical ABI**: Implements the Component Model Canonical ABI for type encoding/decoding
- **Component Model API**: High-level interface for working with components

## Development

### Building
```bash
mvn clean install
```

### Running Tests
```bash
mvn test
```

### Code Style
Code style is enforced using Spotless. Auto-format with:
```bash
mvn spotless:apply
```

## References

- [WebAssembly Component Model](https://github.com/WebAssembly/component-model)
- [WIT Specification](https://github.com/WebAssembly/component-model/tree/main/design/mvp)
- [Canonical ABI](https://github.com/WebAssembly/component-model/blob/main/design/mvp/CanonicalABI.md)
