# Software Composition Analysis

The following table shows the software composition analysis support of the metaeffekt 
inventory plugin. The plugin is driven by project work and regularly benchmarked against 
other tools including Syft.

| Supported Ecosystem                               | metæffekt   | Syft   |
|---------------------------------------------------|-------------|--------|
| Alpine (apk)                                      | ✓           | ✓      |
| C (Conan)                                         | ✓           | ✓      |
| C++ (Conan)                                       | ✓           | ✓      |
| Dart (pubs)                                       | ✓           | ✓      |
| Debian (dpkg)                                     | ✓           | ✓      |
| Dotnet (deps.json)                                | ✓           | ✓      |
| Objective-C (cocoapods)                           | ✓           | ✓      |
| Eclipse Bundle (about.html/ini/properties...)     | ✓           | ✗      |
| Elixir (Mix)                                      | ✓           | ✓      |
| Erlang (rebar3)                                   | ✗           | ✓      |
| Go (go.mod)                                       | ✓           | ✓      |
| Go (Go binaries)                                  | ✗           | ✓      |
| Haskell (cabal, stack)                            | ✗           | ✓      |
| Java (jar, ear, war, par, sar, nar, native-image) | ✓           | ✓      |
| Java Runtime                                      | ✓           | ✗      |
| JavaScript (npm)                                  | ✓           | ✓      |
| JavaScript (yarn)                                 | ✓           | ✓      |
| JavaScript (bower)                                | ✓           | ✗      |
| Jenkins Plugins (jpi, hpi)                        | ✓           | ✓      |
| Jetty (version.txt)                               | ✓           | ✗      |
| Linux kernel archives (vmlinz)                    | ✓           | ✓      |
| Linux kernel modules (ko)                         | ✓           | ✓      |
| Maven-based Source Projects (pom.xml)             | ✗ (planned) | ✗      |
| .msi files                                        | ✗ (planned) | ✗      |
| Nix (outputs in /nix/store)                       | ✗           | ✓      |
| Nextcloud (appinfo/info.xml)                      | ✓           | ✗      |
| Node Runtime (node_version.h)                     | ✓           | ✗      |
| Nordeck App (licenses.json)                       | ✓           | ✗      |
| PHP (composer)                                    | ✓           | ✓      |
| PWA (manifest.json)                               | ✓           | ✗      |
| Python (wheel)                                    | ✓           | ✓      |
| Python (egg)                                      | ✗           | ✓      |
| Python (poetry)                                   | ✗           | ✓      |
| Python (requirements.txt)                         | ✗           | ✓      |
| Red Hat (rpm)                                     | ✓           | ✓      |
| RPM Metadata                                      | ✗ (planned) | ✗      |
| Ruby (gem)                                        | ✓           | ✓      |
| Rust (cargo.lock)                                 | ✗ (planned) | ✓      |
| Swift (cocoapods, swift-package-manager)          | ✗           | ✓      |
| Web Application (web.xml)                         | ✓           | ✗      |
| Wordpress plugins                                 | ✗           | ✓      |
| XWikiExtension (.xed)                             | ✓           | ✗      |
| **Total included**                                | **30**      | **29** |
| **Total missing**                                 | **13**      | **13** |