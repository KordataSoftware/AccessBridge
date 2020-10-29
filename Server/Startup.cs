using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Newtonsoft.Json;
using Serilog;

namespace Kordata.AccessBridge.Server
{
    public class Startup
    {
        public Startup(IConfiguration configuration)
        {
            Configuration = configuration;

            JsonConvert.DefaultSettings = () => SchemaSerializer.Settings;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        // For more information on how to configure your application, visit https://go.microsoft.com/fwlink/?LinkID=398940
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddHttpClient("WatchWebhooks");

            services.Configure<CouchbaseConfig>(Configuration.GetSection(CouchbaseConfig.WatchDatabase));
            services.Configure<AccessConfig>(Configuration.GetSection(AccessConfig.MicrosoftAccess));
            services.Configure<FileConfig>(Configuration.GetSection(FileConfig.FileUpload));

            services.AddSingleton<ICouchbaseLiteFactory, CouchbaseLiteFactory>();
            services.AddSingleton<IAccessConnectionFactory, AccessConnectionFactory>();

            services.AddTransient<IFileRepository, FileRepository>();
            services.AddTransient<IWatchRepository, WatchRepository>();

            services.AddHostedService<TableWatcher>();

            services.AddControllersWithViews();
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseSerilogRequestLogging();

            app.UseRouting();
        }
    }
}