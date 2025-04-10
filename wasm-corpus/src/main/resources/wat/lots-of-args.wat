(module

    (func $test (export "test") (param $p1 i32) (param $p2 i32)
        (result i32) (call $lotsofargs
            ;; includes a mix of arg types
            (local.get $p1) (i64.const 0) (f32.const 0) (f64.const 0) (ref.null func) (ref.null extern) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)

            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)

            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)

            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0) (i32.const 0)
            (local.get $p2) ;; $a299

        )
    )

    ;; add( a00, ..., a299 ) returns a00+a299
    (func $lotsofargs (export "add")

      ;; includes a mix of arg types
      (param $a000 i32) (param $a001 i64) (param $a002 f32) (param $a003 f64) (param $a004 funcref) (param $a005 externref) (param $a006 i32) (param $a007 i32) (param $a008 i32) (param $a009 i32)

      (param $a010 i32) (param $a011 i32) (param $a012 i32) (param $a013 i32) (param $a014 i32) (param $a015 i32) (param $a016 i32) (param $a017 i32) (param $a018 i32) (param $a019 i32)
      (param $a020 i32) (param $a021 i32) (param $a022 i32) (param $a023 i32) (param $a024 i32) (param $a025 i32) (param $a026 i32) (param $a027 i32) (param $a028 i32) (param $a029 i32)
      (param $a030 i32) (param $a031 i32) (param $a032 i32) (param $a033 i32) (param $a034 i32) (param $a035 i32) (param $a036 i32) (param $a037 i32) (param $a038 i32) (param $a039 i32)
      (param $a040 i32) (param $a041 i32) (param $a042 i32) (param $a043 i32) (param $a044 i32) (param $a045 i32) (param $a046 i32) (param $a047 i32) (param $a048 i32) (param $a049 i32)
      (param $a050 i32) (param $a051 i32) (param $a052 i32) (param $a053 i32) (param $a054 i32) (param $a055 i32) (param $a056 i32) (param $a057 i32) (param $a058 i32) (param $a059 i32)
      (param $a060 i32) (param $a061 i32) (param $a062 i32) (param $a063 i32) (param $a064 i32) (param $a065 i32) (param $a066 i32) (param $a067 i32) (param $a068 i32) (param $a069 i32)
      (param $a070 i32) (param $a071 i32) (param $a072 i32) (param $a073 i32) (param $a074 i32) (param $a075 i32) (param $a076 i32) (param $a077 i32) (param $a078 i32) (param $a079 i32)
      (param $a080 i32) (param $a081 i32) (param $a082 i32) (param $a083 i32) (param $a084 i32) (param $a085 i32) (param $a086 i32) (param $a087 i32) (param $a088 i32) (param $a089 i32)
      (param $a090 i32) (param $a091 i32) (param $a092 i32) (param $a093 i32) (param $a094 i32) (param $a095 i32) (param $a096 i32) (param $a097 i32) (param $a098 i32) (param $a099 i32)

      (param $a100 i32) (param $a101 i32) (param $a102 i32) (param $a103 i32) (param $a104 i32) (param $a105 i32) (param $a106 i32) (param $a107 i32) (param $a108 i32) (param $a109 i32)
      (param $a110 i32) (param $a111 i32) (param $a112 i32) (param $a113 i32) (param $a114 i32) (param $a115 i32) (param $a116 i32) (param $a117 i32) (param $a118 i32) (param $a119 i32)
      (param $a120 i32) (param $a121 i32) (param $a122 i32) (param $a123 i32) (param $a124 i32) (param $a125 i32) (param $a126 i32) (param $a127 i32) (param $a128 i32) (param $a129 i32)
      (param $a130 i32) (param $a131 i32) (param $a132 i32) (param $a133 i32) (param $a134 i32) (param $a135 i32) (param $a136 i32) (param $a137 i32) (param $a138 i32) (param $a139 i32)
      (param $a140 i32) (param $a141 i32) (param $a142 i32) (param $a143 i32) (param $a144 i32) (param $a145 i32) (param $a146 i32) (param $a147 i32) (param $a148 i32) (param $a149 i32)
      (param $a150 i32) (param $a151 i32) (param $a152 i32) (param $a153 i32) (param $a154 i32) (param $a155 i32) (param $a156 i32) (param $a157 i32) (param $a158 i32) (param $a159 i32)
      (param $a160 i32) (param $a161 i32) (param $a162 i32) (param $a163 i32) (param $a164 i32) (param $a165 i32) (param $a166 i32) (param $a167 i32) (param $a168 i32) (param $a169 i32)
      (param $a170 i32) (param $a171 i32) (param $a172 i32) (param $a173 i32) (param $a174 i32) (param $a175 i32) (param $a176 i32) (param $a177 i32) (param $a178 i32) (param $a179 i32)
      (param $a180 i32) (param $a181 i32) (param $a182 i32) (param $a183 i32) (param $a184 i32) (param $a185 i32) (param $a186 i32) (param $a187 i32) (param $a188 i32) (param $a189 i32)
      (param $a190 i32) (param $a191 i32) (param $a192 i32) (param $a193 i32) (param $a194 i32) (param $a195 i32) (param $a196 i32) (param $a197 i32) (param $a198 i32) (param $a199 i32)

      (param $a200 i32) (param $a201 i32) (param $a202 i32) (param $a203 i32) (param $a204 i32) (param $a205 i32) (param $a206 i32) (param $a207 i32) (param $a208 i32) (param $a209 i32)
      (param $a210 i32) (param $a211 i32) (param $a212 i32) (param $a213 i32) (param $a214 i32) (param $a215 i32) (param $a216 i32) (param $a217 i32) (param $a218 i32) (param $a219 i32)
      (param $a220 i32) (param $a221 i32) (param $a222 i32) (param $a223 i32) (param $a224 i32) (param $a225 i32) (param $a226 i32) (param $a227 i32) (param $a228 i32) (param $a229 i32)
      (param $a230 i32) (param $a231 i32) (param $a232 i32) (param $a233 i32) (param $a234 i32) (param $a235 i32) (param $a236 i32) (param $a237 i32) (param $a238 i32) (param $a239 i32)
      (param $a240 i32) (param $a241 i32) (param $a242 i32) (param $a243 i32) (param $a244 i32) (param $a245 i32) (param $a246 i32) (param $a247 i32) (param $a248 i32) (param $a249 i32)
      (param $a250 i32) (param $a251 i32) (param $a252 i32) (param $a253 i32) (param $a254 i32) (param $a255 i32) (param $a256 i32) (param $a257 i32) (param $a258 i32) (param $a259 i32)
      (param $a260 i32) (param $a261 i32) (param $a262 i32) (param $a263 i32) (param $a264 i32) (param $a265 i32) (param $a266 i32) (param $a267 i32) (param $a268 i32) (param $a269 i32)
      (param $a270 i32) (param $a271 i32) (param $a272 i32) (param $a273 i32) (param $a274 i32) (param $a275 i32) (param $a276 i32) (param $a277 i32) (param $a278 i32) (param $a279 i32)
      (param $a280 i32) (param $a281 i32) (param $a282 i32) (param $a283 i32) (param $a284 i32) (param $a285 i32) (param $a286 i32) (param $a287 i32) (param $a288 i32) (param $a289 i32)
      (param $a290 i32) (param $a291 i32) (param $a292 i32) (param $a293 i32) (param $a294 i32) (param $a295 i32) (param $a296 i32) (param $a297 i32) (param $a298 i32) (param $a299 i32)

      (result i32)
        (i32.add (local.get $a000) (local.get $a299))
    )
)
