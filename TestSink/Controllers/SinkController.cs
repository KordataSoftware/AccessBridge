using Microsoft.AspNetCore;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json.Linq;

namespace Kordata.AccessBridge.TestSink
{
    public class SinkController : Controller
    {
        private readonly ILogger logger;
        public SinkController(ILogger<SinkController> logger)
        {
            this.logger = logger;
        }

        [HttpPost("/v1/sink")]
        public IActionResult Sink([FromBody]JArray data)
        {
            logger.LogInformation(data.ToString());

            return NoContent();
        }
    }
}