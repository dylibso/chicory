(module
  (type (;0;) (func (result i32 i32)))
  (type (;1;) (func (result f64 f32)))
  (func (;0;) (type 1) (result f64 f32)
    f64.const 0x1.25448486d4b6fp+53 (;=1.03184e+16;)
    f32.const 0x1.e2acd2p+91 (;=4.66815e+27;))
  (export "repro" (func 0))
)
