require_relative "common/common"
require "json"

RELEASE_CONFIG = {
  "google-client-id" => "887440561153-pb9gmue2cbbs2gbn9nkr35g0ifpvb8g5.apps.googleusercontent.com",
}

def build_for_release()
  c = Common.new
  c.status "Prepping release directory..."
  c.run_inline %W{rm -rf release}
  c.run_inline %W{mkdir -p release/target}
  c.status "Creating build container..."
  env = c.load_env
  cname = "#{env.namespace}-release-builder"
  c.run_inline %W{
    docker create --name #{cname}
      -v jars:/root/.m2
      -w /w
      clojure
      lein with-profile deploy cljsbuild once
  }
  at_exit { c.run %W{docker rm -f #{cname}} }
  env.source_file_paths.each do |src_path|
    c.pipe(
      %W{tar -c #{src_path}},
      %W{docker cp - #{cname}:/w}
    )
  end
  c.status "Building..."
  c.run_inline %W{docker start -a #{cname}}
  c.status "Copying artifacts..."
  c.run_inline %W{docker cp #{cname}:/w/target/cljsbuild-main.js release/target/compiled.js}
  c.status "Copying static files..."
  Dir.foreach(env.static_file_src) do |entry|
    unless entry.start_with?(".")
      item = "#{env.static_file_src}/#{entry}"
      c.run_inline %W{cp -R #{item} release}
    end
  end
end

def configure_release()
  c = Common.new
  config_file = "release/config.json"
  File.write(config_file, JSON.pretty_generate(RELEASE_CONFIG) + "\n")
  c.status "Configuration written to #{config_file}."
end

def deploy()
  c = Common.new
  unless Dir.exist?("release")
    c.error "Build release first."
    exit 1
  end
  unless File.exist?("release/config.json")
    c.error "Configure release first."
    exit 1
  end
  c.status "Deploying to App Engine..."
  c.run_inline %W{gcloud app deploy --project allofus-164617 --quiet}
end

def test_release_image()
  c = Common.new
  env = c.load_env
  unless Dir.exist?("release")
    c.error "Build release first."
    exit 1
  end
  unless File.exist?("release/config.json")
    c.error "Configure release first."
    exit 1
  end
  c.status "Building docker image..."
  c.run_inline %W{docker build -t #{env.namespace} -f Dockerfile .}
  c.status "Running at http://localhost:3449/ ..."
  c.run_inline %W{docker run --rm -it -p 3449:8080 #{env.namespace}}
end

Common.register_command({
  :invocation => "build-for-release",
  :description => "Builds a version of the code suitible for deployment.",
  :fn => Proc.new { |*args| build_for_release(*args) }
})

Common.register_command({
  :invocation => "configure-release",
  :description => "Configures the build for release.",
  :fn => Proc.new { |*args| configure_release(*args) }
})

Common.register_command({
  :invocation => "deploy",
  :description => "Deploys the application to Google App Engine.",
  :fn => Proc.new { |*args| deploy(*args) }
})

Common.register_command({
  :invocation => "test-release-image",
  :description => "Builds a releasable image and runs it as a container. Useful for testing a" \
    " release.",
  :fn => Proc.new { |*args| test_release_image(*args) }
})
