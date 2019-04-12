namespace Kordata.AccessBridge.Server
{
    public static class StringExtensions
    {
        public static string ToParameterName(this string str)
        {
            return str.Replace(" ", "").Replace("/", "").Replace("-", "");
        }
    }
}