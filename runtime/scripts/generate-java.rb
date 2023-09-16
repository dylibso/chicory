require 'json'

def java_header(class_name)
<<-HEADER

package com.dylibso.chicory.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import org.junit.Test;

public class #{class_name} {

HEADER
end

def camelize(str)
  str.split(/_|-/).map(&:capitalize).join
end

def generate_test(inputs)
    inputs.each do |input_file|
        file_name = File.basename(input_file)
        base_name = file_name.split(".").first
        spec_name = camelize(base_name)
        class_name = "SpecV1#{spec_name}Test"
        wasm_file = "src/test/resources/wasm/specv1/#{base_name}.0.wasm"

        File.open("src/test/java/com/dylibso/chicory/runtime/#{class_name}.java", "w") do |out|
            out.puts java_header(class_name)
            out.puts "\t@Test"
            out.puts "\tpublic void testFunc() {"
            out.puts "\t\tvar instance = Module.build(\"#{wasm_file}\").instantiate();"
            ast = JSON.parse(IO.read(input_file))
            exports = Set.new

            ast['commands'].each do |c|
              next unless c['type'] == 'assert_return'

              action = c['action']
              next unless action['type'] == 'invoke'

              field = action['field']

              unless exports.include?(field)
                var_name = field.gsub(/-|_|\./, '')
                # put an "x" at the beginning of the var to make it valid java
                var_name = "x#{var_name}" if var_name[0] =~ /\d/
                out.puts "\t\tvar #{var_name} = instance.getExport(\"#{field}\");"
                exports << field
              end

              args = action['args'].map { |a| "Value.#{a['type']}(#{a['value']}L & 0xFFFFFFFFL)" }

              expected = c['expected'].first
              expected = expected ? "(int)(#{expected['value']}L & 0xFFFFFFFFL)" : 'null'

              var_name = field.gsub(/_|-|\./, '')
              # put an "x" at the beginning of the var to make it valid java
              var_name = "x#{var_name}" if var_name[0] =~ /\d/
              out.puts "\t\tassertEquals(#{expected}, #{var_name}.apply(#{args.join(', ')}).asInt());"
            end
            out.puts "\t}"
          out.puts "}"
        end
    end
end

dir = File.expand_path(File.dirname(File.dirname(__FILE__)))
#inputs = Dir.glob(File.join(dir, "src/test/resources/wasm/specv1", "*.json"))
#generate_test(inputs)
generate_test(["src/test/resources/wasm/specv1/i32.json"])
