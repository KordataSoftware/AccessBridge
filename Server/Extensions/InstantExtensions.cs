using NodaTime;
using NodaTime.Text;

namespace Kordata.AccessBridge.Server
{
    public static class LocalDateTimeExtensions
    {
        private static LocalDateTimePattern accessPattern = LocalDateTimePattern.CreateWithInvariantCulture("yyyy-MM-dd HH:mm:ss");
        public static string ToAccessDateTime(this LocalDateTime localDateTime)
        {
            return accessPattern.Format(localDateTime);
        }
    }
}