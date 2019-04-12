using System;
using System.Collections.Generic;
using System.Reflection;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using NodaTime;
using NodaTime.Serialization.JsonNet;

namespace Kordata.AccessBridge.Server
{
    public class SchemaSerializer
    {
        public static JsonSerializer Instance =
            JsonSerializer.Create(Settings);

        public static JsonSerializerSettings Settings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver(),
            Converters = new List<JsonConverter>
            {
                new TypeConverter()
            },
            DateParseHandling = DateParseHandling.None
        }.ConfigureForNodaTime(DateTimeZoneProviders.Tzdb);
    }
}