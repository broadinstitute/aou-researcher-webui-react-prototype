require_relative "common/common"

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

def deploy()
  c = Common.new
  c.status "Deploying to App Engine..."
  c.run_inline %W{gcloud app deploy --project allofus-164617}
end

Common.register_command({
  :invocation => "build-for-release",
  :description => "Builds a version of the code suitible for deployment.",
  :fn => Proc.new { |*args| build_for_release(*args) }
})

Common.register_command({
  :invocation => "deploy",
  :description => "Deploys the application to Google App Engine.",
  :fn => Proc.new { |*args| deploy(*args) }
})
