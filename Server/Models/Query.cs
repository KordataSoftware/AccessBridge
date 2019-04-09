
using System.Collections.Generic;

namespace Kordata.AccessBridge.Server
{
    public class Query
    {
        public string Command { get; set; }
        public List<object> Parameters { get; set; }
    }
}