require_relative "common/common"
require "json"

DEV_CONFIG = {
  "google-client-id" => "887440561153-pb9gmue2cbbs2gbn9nkr35g0ifpvb8g5.apps.googleusercontent.com",
}

def write_config()
  c = Common.new
  env = c.load_env
  config = JSON.pretty_generate(DEV_CONFIG)
  sh_command = "cat > /w/resources/public/config.json"
  c.pipe(
    %W{echo #{config}},
    %W{docker run --rm -i} + c.sf.get_volume_mounts + %W{alpine sh -c #{sh_command}}
  )
  c.status "config.json written to container."
end

def start_dev()
  c = Common.new
  env = c.load_env
  c.sf.maybe_start_file_syncing
  c.status "Writing config to container..."
  write_config
  unless c.docker.image_exists?("clojure:rlwrap")
    c.error "Image clojure:rlwrap does not exist. Building..."
    c.run_inline %W{
      docker build -t clojure:rlwrap -f src/container/clojure-rlwrap/Dockerfile
        src/container/clojure-rlwrap
    }
  end
  c.status "Starting figwheel. Wait for prompt before connecting with a browser..."
  docker_run = %W{
    docker run --name #{env.namespace}-figwheel
      --rm -it
      -w /w
      -p 3449:3449
      -v jars:/root/.m2
  }
  docker_run += c.sf.get_volume_mounts
  cmd = "sleep 1; rlwrap lein figwheel"
  docker_run += %W{clojure:rlwrap bash -c #{cmd}}
  c.run_inline docker_run
end

Common.register_command({
  :invocation => "startdev",
  :description => "Starts the development compiler and server.",
  :fn => Proc.new { |*args| start_dev(*args) }
})
