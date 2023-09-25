require 'json'

def java_header(class_name)
  <<-HEADER

  package com.dylibso.chicory.runtime;

  import java.math.BigInteger;
  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertThrows;
  import com.dylibso.chicory.wasm.types.Value;
  import com.dylibso.chicory.wasm.types.ValueType;
  import org.junit.Test;

  public class #{class_name} {

    public static long longVal(String v) {
      return new BigInteger(v).longValue();
    }

    public static float floatVal(String s) {
      return Float.intBitsToFloat(Integer.parseInt(s));
    }

    public static double doubleVal(String s) {
      return Double.longBitsToDouble(longVal(s));
    }

  HEADER
end

# snake, kebab, and dot case to camel case
def camelize(str)
  str.split(/_|-|\./).map(&:capitalize).join
end

def val_to_value(val)
  case val['type']
  when 'i32'
    "Value.i32(#{val_to_java_value(val)})"
  when 'i64'
    "Value.i64(#{val_to_java_value(val)})"
  when 'f32'
    "Value.f32(longVal(\"#{val['value']}\"))"
  when 'f64'
    "Value.f64(longVal(\"#{val['value']}\"))"
  else
    raise ArgumentError, "Unknown type for val #{val}"
  end
end

def val_to_java_value(val)
  case val['type']
  when 'i32'
    "(int)(#{val['value']}L & 0xFFFFFFFFL)"
  when 'i64'
    "longVal(\"#{val['value']}\")"
  when 'f32'
    "floatVal(\"#{val['value']}\")"
  when 'f64'
    "doubleVal(\"#{val['value']}\")"
  else
    raise ArgumentError, "Unknown type for val #{val}"
  end
end



def cast(val)
  return "" if val.nil?

  case val['type']
  when 'i32'
    ".asInt()"
  when 'i64'
    ".asLong()"
  when 'f32'
    ".asFloat()"
  when 'f64'
    ".asDouble()"
  else
    raise ArgumentError, "Unknown type for val #{val}"
  end
end

# parse the flat list of commands into a list of
# modules with their assertions
def parse(ast)
  mods = {}
  current_filename = nil
  ast['commands'].map do |c|
    current_filename = c['filename'] ||= current_filename
    current_mod = mods[current_filename] ||= { 'asserts' => [], 'filename' => current_filename }
    next if c['type'] == 'module'

    current_mod['asserts'] << c
  end
  mods.values
end

def generate_test(inputs)
  inputs.each do |input_file|
    file_name = File.basename(input_file)
    base_name = file_name.split('.').first
    spec_name = camelize(base_name)
    class_name = "SpecV1#{spec_name}Test"

    File.open("src/test/java/com/dylibso/chicory/runtime/#{class_name}.java", 'w') do |out|
      out.puts java_header(class_name)
      ast = JSON.parse(IO.read(input_file))
      mods = parse(ast)

      mods.each do |mod|
        test_name = camelize(mod['filename'])
        next unless mod['filename'].downcase.end_with? '.wasm'

        out.puts "\t@Test"
        out.puts "\tpublic void test#{test_name}() {"

        exports = Set.new
        wasm_file = "src/test/resources/wasm/specv1/#{mod['filename']}"
        out.puts "\t\tvar instance = Module.build(\"#{wasm_file}\").instantiate();"
        mod['asserts'].each do |assertion|
          # TODO: fix when we support other types of assertions
          next unless assertion['type'] == 'assert_return'

          action = assertion['action']
          next unless action['type'] == 'invoke'

          field = action['field']
          unless exports.include?(field)
            var_name = field.gsub(/-|_|\./, '')
            # put an "x" at the beginning of the var to make it valid java
            var_name = "x#{var_name}" if var_name[0] =~ /\d/
            out.puts "\t\tvar #{var_name} = instance.getExport(\"#{field}\");"
            exports << field
          end

          args = action['args'].map { |v| val_to_value(v) }
          expected_val = assertion['expected'].first
          expected = expected_val ? val_to_java_value(expected_val) : 'null'

          var_name = field.gsub(/_|-|\./, '')
          # put an "x" at the beginning of the var to make it valid java
          var_name = "x#{var_name}" if var_name[0] =~ /\d/
          case expected_val && expected_val['type']
          when 'i32', 'i64', nil
            out.puts "\t\tassertEquals(#{expected}, #{var_name}.apply(#{args.join(', ')})#{cast(expected_val)});"
          when 'f32', 'f64'
            out.puts "\t\tassertEquals(#{expected}, #{var_name}.apply(#{args.join(', ')})#{cast(expected_val)}, 0.0);"
          else
            raise ArgumentError, "Can't generate an assertion for expected val: #{expected_val}"
          end

        end
        out.puts "\t}"
      end
      out.puts '}'
    end
  end
end

dir = File.expand_path(File.dirname(File.dirname(__FILE__)))

# We can use this when all the specs work, for now we're doing it one file at a time
# inputs = Dir.glob(File.join(dir, "src/test/resources/wasm/specv1", "*.json"))
# generate_test(inputs)

# I am leaving partially working specs commented out
# You can uncomment to work on it, but leave it commented until it fully passes
generate_test([
    'src/test/resources/wasm/specv1/i32.json',
    'src/test/resources/wasm/specv1/i64.json',
#    'src/test/resources/wasm/specv1/local_get.json',
    'src/test/resources/wasm/specv1/memory.json',
])
