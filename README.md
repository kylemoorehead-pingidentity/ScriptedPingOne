# ScriptedPingOne

## Table of Contents
- [Prerequisites](#prerequisites)
- [Download Scripts and Configuration Files](#download-scripts-and-configuration-files)
- [Install ForgeRock RCS](#install-forgerock-rcs)


### Prerequisites
- A server on which to install [ForgeRock Remote Connector Server (RCS)](https://backstage.forgerock.com/docs/idm/7.1/connector-reference/install-connector-server.html)
  - Windows Server or Linux works; the installation instructions below will cover both
- Install [OpenJDK 11.0.2 (Build 11.0.2+9)](https://jdk.java.net/archive/#:~:text=11.0.2%20(build%2011.0.2%2B9)) on your server and add it to the Path \
_Note: Other JDK versions may work, but a couple that did not were found while testing this. Feel free to try other JDKs but fail back to OpenJDK 11.0.2+9 if you run into issues._
  - Download either the .zip for Windows or .tar.gz for Linux from the [OpenJDK archive](https://jdk.java.net/archive/#:~:text=11.0.2%20(build%2011.0.2%2B9)).
  - Extract the JDK and add JAVA_HOME to the PATH
    <details>
      <summary>Windows</summary>
      
    PowerShell
    ```PowerShell
    Expand-Archive -Path "C:\Users\<username>\Downloads\openjdk-11.0.2_windows-x64_bin.zip" -DestinationPath "C:\Program Files\Java"
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-11.0.2")
    [System.Environment]::SetEnvironmentVariable("Path", [System.Environment]::GetEnvironmentVariable('Path', [System.EnvironmentVariableTarget]::Machine) + ";$($env:JAVA_HOME)\bin")
    ```
    </details>
    <details>
      <summary>Linux</summary>
      
    ```console
    whoami
    ```
    </details>

### Download Scripts and Configuration Files

### Install ForgeRock RCS
- Download the Remote Connector Server (Java) from [ForgeRock Backstage](https://backstage.forgerock.com/downloads/browse/idm/featured/connectors).
- Extract the RCS to a new directory
<details>
  <summary>Windows</summary>
  
  ```PowerShell
  # Expand-Archive creates the DestinationPath if it does not exist
  Expand-Archive -Path C:\Users\<username>\Downloads\openicf-zip-1.5.20.15.zip -DestinationPath ~\RCS
  cd ~\RCS
  ```
</details>
<details>
  <summary>Linux</summary>
  
  ```console
  whoami
  ```
</details>
- 
