
using System.Data.Common;
using System.Linq;
using Newtonsoft.Json.Linq;

namespace Kordata.AccessBridge.Server
{
    public static class DbDataReaderExtensions
    {
        public static JObject GetJObject(this DbDataReader reader)
        {
            var properties = Enumerable.Range(0, reader.FieldCount)
                .Select(i => new JProperty(reader.GetName(i), reader.GetValue(i)));

            return new JObject(properties);
        }
    }

}