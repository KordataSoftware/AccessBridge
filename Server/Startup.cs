using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;
using Newtonsoft.Json;
using Microsoft.AspNetCore.Mvc;

namespace Kordata.AccessBridge.Server
{
    public class Startup
    {
        private readonly IConfiguration configuration;
        private readonly IHostingEnvironment environment;

        public Startup(IConfiguration configuration, IHostingEnvironment environment)
        {
            this.configuration = configuration;
            this.environment = environment;

            JsonConvert.DefaultSettings = () => SchemaSerializer.Settings;
        }

        // This method gets called by the runtime. Use this method to add services to the container.
        // For more information on how to configure your application, visit https://go.microsoft.com/fwlink/?LinkID=398940
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddMvc().SetCompatibilityVersion(CompatibilityVersion.Version_2_2);

            services.AddHttpClient("WatchWebhooks");

            services.Configure<CouchbaseConfig>(configuration.GetSection(CouchbaseConfig.WatchDatabase));
            services.Configure<AccessConfig>(configuration.GetSection(AccessConfig.MicrosoftAccess));
            services.Configure<FileConfig>(configuration.GetSection(FileConfig.FileUpload));

            services.AddSingleton<ICouchbaseLiteFactory, CouchbaseLiteFactory>();
            services.AddSingleton<IAccessConnectionFactory, AccessConnectionFactory>();

            services.AddTransient<IFileRepository, FileRepository>();
            services.AddTransient<IWatchRepository, WatchRepository>();

            services.AddHostedService<TableWatcher>();
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IHostingEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseMvc();
        }
    }
}