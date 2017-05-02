require_relative "common/common"
require "json"

DEV_CONFIG = {
  "google-client-id" => "887440561153-pb9gmue2cbbs2gbn9nkr35g0ifpvb8g5.apps.googleusercontent.com",
}

def write_config()
  c = Common.new
  env = c.load_env
  config = JSON.pretty_generate(DEV_CONFIG)
  bash_command = "cat > /w/resources/public/config.json"
  c.pipe(
    %W{echo #{config}},
    %W{docker exec -i #{env.namespace}-rsync bash -c #{bash_command}}
  )
  c.status "config.json written to container."
end

def start_dev()
  c = Common.new
  env = c.load_env
  c.status "Starting rsync container..."
  c.sf.start_rsync_container
  at_exit { c.sf.stop_rsync_container }
  c.status "Performing initial file sync..."
  c.sf.perform_initial_sync
  write_config
  c.sf.start_watching_sync
  c.status "Watching source files. See log at #{c.sf.log_file_name}."
  unless c.docker.image_exists?("clojure:rlwrap")
    c.error "Image clojure:rlwrap does not exist. Building..."
    c.run_inline %W{
      docker build -t clojure:rlwrap -f src/container/clojure-rlwrap/Dockerfile
        src/container/clojure-rlwrap
    }
  end
  c.status "Starting figwheel. Wait for prompt before connecting with a browser..."
  cmd = "sleep 1; rlwrap lein figwheel"
  c.run_inline %W{
    docker run --name #{env.namespace}-figwheel
      --rm -it
      -w /w -v #{c.sf.shared_vol}:/w
      -p 3449:3449
      -v jars:/root/.m2
      clojure:rlwrap
      bash -c #{cmd}
  }
end

Common.register_command({
  :invocation => "startdev",
  :description => "Starts the development compiler and server.",
  :fn => Proc.new { |*args| start_dev(*args) }
})
