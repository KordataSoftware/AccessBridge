using System;
using System.Reflection;
using Newtonsoft.Json;

namespace Kordata.AccessBridge.Server
{
    public class TypeConverter : JsonConverter
    {
        public TypeConverter()
        {
        }

        public override void WriteJson(JsonWriter writer, object value, JsonSerializer serializer)
        {
            switch ((Type)value)
            {
                case Type intType when intType == typeof(Int32):
                    writer.WriteValue("int");
                    break;
                case Type stringType when stringType == typeof(String):
                    writer.WriteValue("string");
                    break;
                case Type boolType when boolType == typeof(Boolean):
                    writer.WriteValue("boolean");
                    break;
                case Type dateTimeType when dateTimeType == typeof(DateTime):
                    writer.WriteValue("dateTime");
                    break;
                case Type decimalType when decimalType == typeof(Decimal):
                    writer.WriteValue("decimal");
                    break;
                case Type shortType when shortType == typeof(Int16):
                    writer.WriteValue("short");
                    break;
                case Type doubleType when doubleType == typeof(Double):
                    writer.WriteValue("double");
                    break;
                case Type byteType when byteType == typeof(Byte):
                    writer.WriteValue("byte");
                    break;
                default:
                    writer.WriteValue("unknown");
                    break;
            }
        }

        public override object ReadJson(JsonReader reader, Type objectType, object existingValue, JsonSerializer serializer)
        {
            throw new NotImplementedException();
        }

        public override bool CanConvert(Type objectType)
        {
            return typeof(Type).GetTypeInfo().IsAssignableFrom(objectType.GetTypeInfo());
        }
    }
}