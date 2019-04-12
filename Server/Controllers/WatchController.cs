using System.Data.Odbc;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using MoreLinq;
using Newtonsoft.Json.Linq;
using System;
using System.Data.Common;
using System.Collections.Generic;
using Microsoft.Extensions.Logging;

namespace Kordata.AccessBridge.Server
{
    public class WatchController : BaseController
    {
        private readonly IWatchRepository watchRepository;

        public WatchController(ILogger<WatchController> logger,
            IAccessConnectionFactory connectionFactory,
            IWatchRepository watchRepository)
            : base(logger, connectionFactory)
        {
            this.watchRepository = watchRepository;
        }

        [HttpPut("/v1/watches/{id}")]
        public IActionResult PutWatch(string id, [FromBody]Watch watch)
        {
            watch.Id = id;

            watchRepository.Put(watch);

            return NoContent();
        }

        [HttpPost("/v1/watches")]
        public IActionResult PostWatch([FromBody]Watch watch)
        {
            watch.Id = Guid.NewGuid().ToString();

            var createdId = watchRepository.Put(watch);

            var response = new JObject();
            response["id"] = createdId;

            return new JsonResult(response);
        }

        [HttpGet("/v1/watches/{id}")]
        public IActionResult GetWatch(string id)
        {
            var watch = watchRepository.Get(id);

            return new JsonResult(JObject.FromObject(watch));
        }

        [HttpDelete("/v1/watches/{id}")]
        public IActionResult DeleteWatch(string id)
        {
            if (watchRepository.Delete(id))
            {
                return NoContent();
            }

            return NotFound();
        }

        [HttpGet("/v1/watches")]
        public IActionResult GetWatches([FromQuery]string database)
        {
            List<Watch> watches;
            if (!string.IsNullOrEmpty(database))
            {
                watches = watchRepository.GetForDatabase(database);
            }
            else
            {
                watches = watchRepository.GetAll();
            }

            return new JsonResult(JArray.FromObject(watches));
        }

        [HttpDelete("/v1/watches")]
        public IActionResult DeleteWatches([FromQuery]string database)
        {
            if (!string.IsNullOrEmpty(database))
            {
                watchRepository.DeleteForDatabase(database);
            }

            return NoContent();
        }
    }
}