# ScriptedPingOne

## Table of Contents
- [Prerequisites](#prerequisites)
- [Download Scripts and Configuration Files](#download-scripts-and-configuration-files)
- [Install ForgeRock RCS](#install-forgerock-rcs)
- [Configure ForgeRock Identity Cloud](#configure-forgerock-identity-cloud)
- [Start the RCS](#start-the-rcs)


### Prerequisites
- A server on which to install [ForgeRock Remote Connector Server (RCS)](https://backstage.forgerock.com/docs/idm/7.1/connector-reference/install-connector-server.html)
  - Windows Server or Linux works; the installation instructions below will cover both.
- Install [OpenJDK 11.0.2 (Build 11.0.2+9)](https://jdk.java.net/archive/#:~:text=11.0.2%20(build%2011.0.2%2B9)) on your server and add it to the Path \
_Note: Other JDK versions may work, but a couple that did not were found while testing this. Feel free to try other JDKs but fail back to OpenJDK 11.0.2+9 if you run into issues._
  - Download either the .zip for Windows or .tar.gz for Linux from the [OpenJDK archive](https://jdk.java.net/archive/#:~:text=11.0.2%20(build%2011.0.2%2B9)).
  - Extract the JDK and add JAVA_HOME to the PATH.
    <details>
      <summary>Windows</summary>
      
    PowerShell
    ```PowerShell
    Expand-Archive -Path "~\Downloads\openjdk-11.0.2_windows-x64_bin.zip" -DestinationPath "C:\Program Files\Java"
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
- Download [this repository](https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/archive/refs/heads/master.zip) and extract it.
  <details>
    <summary>Windows</summary>
  
    ```PowerShell
    # Expand-Archive creates the DestinationPath if it does not exist
    Expand-Archive -Path ~\Downloads\ScriptedPingOne-master.zip -DestinationPath ~\
    Rename-Item ~\ScriptedPingOne-master ScriptedPingOne
    ```
  </details>
  <details>
    <summary>Linux</summary>
    
    ```console
    whoami
    ```
  </details>

### Install ForgeRock RCS
- Download the Remote Connector Server (Java) from [ForgeRock Backstage](https://backstage.forgerock.com/downloads/browse/idm/featured/connectors).
- Extract the RCS to a new directory.
  <details>
    <summary>Windows</summary>
  
    ```PowerShell
    # Expand-Archive creates the DestinationPath if it does not exist
    Expand-Archive -Path ~\Downloads\openicf-zip-1.5.20.15.zip -DestinationPath ~\RCS
    ```
  </details>
  <details>
    <summary>Linux</summary>
    
    ```console
    whoami
    ```
  </details>
  
- Copy the `scripts` and `conf` into your RCS directory
  <details>
    <summary>Windows</summary>
  
    ```PowerShell
    # Use -Force to overwrite any existing files that need to be overwritten
    Copy-Item ~\ScriptedPingOne\src\scripts,~\ScriptedPingOne\src\conf -Destination ~\RCS\openicf\ -Force
    ```
  </details>
  <details>
    <summary>Linux</summary>
    
    ```console
    whoami
    ```
  </details>
  
- Open `~\RCS\openicf\conf\ConnectorServer.properties` in your IDE of choice and update the values enclosed in angle brackets (`< >`) to match your environment
  - On line 42 and 99, replace `<FR-FQDN>` with the FQDN of your ForgeRock Identity Cloud console. E.g. `openam-jckyle1.forgeblocks.com`
  - On line 50, replace `<name>` with whatever you choose. This will be how you identify this RCS in ForgeRock Identity Cloud. Only use lowercase letters and dashes (`-`). Remeber this value as you will need it later while [Configuring ForgeRock Identity Cloud](#configure-forgerock-identity-cloud).
- On line 107, replace `<client-secret>` with whatever you choose. Remember this value as you will need it later while [Configuring ForgeRock Identity Cloud](#configure-forgerock-identity-cloud).

### Configure ForgeRock Identity Cloud
- Sign in to the ForgeRock Identity Cloud Administrative Console.
  
- Using the sidebar, navigate to Identities > Connect. Click + New Connector Server.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-NewRCS.png?raw=true" width="auto">
  </details>

- In the modal, enter the name you set on link 50 of the `ConnectorServer.properties` file. Click Save.
- On the next screen, click Reset on the Client Secret. Set this to the same value as the `<client-secret>` you set on line 107 of the `ConnectorServer.properties` file.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-RCSClientSecret.png?raw=true" width="auto">
  </details>

### Start the RCS
- On the server that you installed ForgeRock RCS, start the RCS.
  <details>
    <summary>Windows</summary>
  
    ```PowerShell
    & ~\RCS\openicf\bin\ConnectorServer.bat /run
    ```
  </details>

- Wait until you see the following message and the return to your ForgeRock Identity Cloud Administrator Console. If you navigate to Identities > Connect, your RCS should now show ![https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-Connected](https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-Connected.png).
```
RCS 1.5.20.15 started.
Press q to shutdown.
```
