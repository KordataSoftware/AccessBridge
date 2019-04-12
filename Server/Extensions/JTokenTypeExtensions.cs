using System.Data.Odbc;
using Newtonsoft.Json.Linq;

namespace Kordata.AccessBridge.Server
{
    public static class JTokenTypeExtensions
    {
        public static OdbcType ToOdbcType(this JTokenType jTokenType)
        {
            switch (jTokenType)
            {
                case JTokenType.Float:
                    return OdbcType.Double;
                case JTokenType.Boolean:
                    return OdbcType.Bit;
                case JTokenType.Integer:
                    return OdbcType.Int;
                case JTokenType.String:
                    return OdbcType.VarChar;
                default:
                    return OdbcType.VarChar;
            }
        }
    }
}