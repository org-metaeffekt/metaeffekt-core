# Software Composition Analysis

The following table shows the software composition analysis support of the metaeffekt 
inventory plugin. The plugin is driven by project work and regularly benchmarked against 
other tools including Syft and (future work) ExtractCode as part of the ScanCode Toolkit.

| Item | Supported Ecosystem                               | metæffekt   | Syft   | ExtractCode |
|------|---------------------------------------------------|-------------|--------|-------------|
| 1    | Alpine (apk)                                      | ✓           | ✓      |             |
| 2    | Bitnami Components                                | ✓           | x      |             |
| 3    | C (Conan)                                         | ✓           | ✓      |             |
| 4    | C++ (Conan)                                       | ✓           | ✓      |             |
| 5    | Dart (pubs)                                       | ✓           | ✓      |             |
| 6    | Debian (dpkg)                                     | ✓           | ✓      |             |
| 7    | Dotnet (deps.json)                                | ✓           | ✓      |             |
| 8    | Objective-C (cocoapods)                           | ✓           | ✓      |             |
| 9    | Eclipse Bundle (about.html/ini/properties...)     | ✓           | ✗      |             |
| 10   | Elixir (Mix)                                      | ✓           | ✓      |             |
| 11   | Erlang (rebar3)                                   | ✗           | ✓      |             |
| 12   | Go (go.mod)                                       | ✓           | ✓      |             |
| 13   | Go (Go binaries)                                  | ✗           | ✓      |             |
| 14   | Haskell (cabal, stack)                            | ✗           | ✓      |             |
| 15   | Java (jar, ear, war, par, sar, nar, native-image) | ✓           | ✓      |             |
| 16   | Java Runtime                                      | ✓           | ✗      |             |
| 17   | JavaScript (npm)                                  | ✓           | ✓      |             |
| 18   | JavaScript (yarn)                                 | ✓           | ✓      |             |
| 19   | JavaScript (bower)                                | ✓           | ✗      |             |
| 20   | Jenkins Plugins (jpi, hpi)                        | ✓           | ✓      |             |
| 21   | Jetty (version.txt)                               | ✓           | ✗      |             |
| 22   | Linux kernel archives (vmlinz)                    | ✓           | ✓      |             |
| 23   | Linux kernel modules (ko)                         | ✓           | ✓      |             |
| 24   | Maven-based Source Projects (pom.xml)             | ✗ (initial) | ✗      |             |
| 25   | .msi/.cab/.exe files                              | ✓           | ✗      |             |
| 26   | Nix (outputs in /nix/store)                       | ✗           | ✓      |             |
| 27   | Nextcloud (appinfo/info.xml)                      | ✓           | ✗      |             |
| 28   | Node Runtime (node_version.h)                     | ✓           | ✗      |             |
| 29   | Nordeck App (licenses.json)                       | ✓           | ✗      |             |
| 30   | PHP (composer)                                    | ✓           | ✓      |             |
| 31   | PWA (manifest.json)                               | ✓           | ✗      |             |
| 32   | Python (wheel)                                    | ✓           | ✓      |             |
| 33   | Python (egg)                                      | ✗           | ✓      |             |
| 34   | Python (poetry)                                   | ✗           | ✓      |             |
| 35   | Python (requirements.txt)                         | ✗           | ✓      |             |
| 36   | Red Hat (rpm)                                     | ✓           | ✓      |             |
| 37   | RPM Metadata                                      | ✗ (planned) | ✗      |             |
| 38   | Ruby (gem)                                        | ✓           | ✓      |             |
| 39   | Rust (cargo.lock)                                 | ✗ (planned) | ✓      |             |
| 40   | Swift (cocoapods, swift-package-manager)          | ✗           | ✓      |             |
| 41   | Web Application (web.xml)                         | ✓           | ✗      |             |
| 42   | Wordpress plugins                                 | ✗           | ✓      |             |
| 43   | XWikiExtension (.xed)                             | ✓           | ✗      |             |
|      | **Total included**                                | **32**      | **29** | TBD         |
|      | **Total missing**                                 | **11**      | **14** | TBD         |