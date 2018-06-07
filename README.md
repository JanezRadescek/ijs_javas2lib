# README #
Commandline (CLI) and GUI interfaces for checking out, filtering, and analysing s2 files. While the general s2 files are supported for most operations, additional module implements PCARD s2 logic and enables time alignment of measurements in those files.

## Modules
### Filters
Filters are the basic building blocks for accessing and modifying s2 files. Filters provide all the functionality behind CLI and GUI,
### CLI
CLI access to filters.
### GUI
GUI access to filters via CLI. GUI produces commands that can be later executed through CLI.
### s2 generator
Used to produce PCARD s2 compatible files for testing time alignment
### Unit tests
Unit tests are defined for the basic functionalities of the 

## Dependencies

### Public libraries
[Apache CLI](http://commons.apache.org/proper/commons-cli/download_cli.cgi)
[Apache IO](https://commons.apache.org/proper/commons-io/download_io.cgi)
[MiG Layout - swing](http://www.migcalendar.com/miglayout/versions/4.0/)
JUnit 4.12

## Private libraries
pcardtimesync  
s2-java-lib
