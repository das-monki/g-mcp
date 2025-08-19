{
  description = "G-MCP: Google Workspace MCP Server";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    devshell = {
      url = "github:numtide/devshell";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    clj-nix = {
      url = "github:jlesquembre/clj-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "aarch64-darwin"
        "x86_64-linux"
        "x86_64-darwin"
        "aarch64-linux"
      ];
      imports = [ inputs.devshell.flakeModule ];

      perSystem =
        {
          config,
          pkgs,
          system,
          ...
        }:
        let
          cljpkgs = inputs.clj-nix.packages.${system};
        in
        {
          packages = {
            default = cljpkgs.mkCljBin {
              projectSrc = ./.;
              name = "g-mcp/g-mcp";
              main-ns = "g-mcp.core";
            };
          };

          devshells.default = {
            name = "G-MCP Development Environment";
            motd = ''
              {202}🔨 Welcome to G-MCP Development Environment{reset}
              $(type -p menu &>/dev/null && menu)
            '';

            env = [
              {
                name = "PROJECT_ROOT";
                value = "$PWD";
              }
              {
                name = "GOOGLE_APPLICATION_CREDENTIALS";
                value = "$PWD/credentials.json";
              }
            ];

            packages = with pkgs; [
              clojure
              openjdk17
              git
              clj-kondo
              cljfmt
              nixfmt-rfc-style
            ];

            commands = [
              {
                name = "update-deps";
                help = "Update dependency lock file";
                command = "${cljpkgs.deps-lock}/bin/deps-lock";
              }
              {
                name = "build";
                help = "Build the G-MCP server";
                command = "nix build";
              }
              {
                name = "format";
                help = "Format Clojure code";
                command = "cljfmt fix";
              }
              {
                name = "check";
                help = "Check if Clojure code is formatted";
                command = "cljfmt check";
              }
              {
                name = "lint";
                help = "Lint Clojure code with clj-kondo";
                command = "clj-kondo --lint src test";
              }
              {
                name = "nix-fmt";
                help = "Format Nix files";
                command = "nixfmt flake.nix";
              }
              {
                name = "run";
                help = "Run the G-MCP server";
                command = "clojure -M:run";
              }
              {
                name = "test";
                help = "Run tests";
                command = "clojure -M:test";
              }
              {
                name = "repl";
                help = "Start Clojure REPL";
                command = "clojure -M:repl";
              }
            ];
          };
        };
    };
}
