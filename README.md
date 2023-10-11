# ScriptedPingOne

## Table of Contents
- [Prerequisites](#prerequisites)
- [Download Scripts and Configuration Files](#download-scripts-and-configuration-files)
- [Install ForgeRock RCS](#install-forgerock-rcs)
- [Configure ForgeRock Identity Cloud](#configure-forgerock-identity-cloud)
- [Start the RCS](#start-the-rcs)
- [Configure the Inbound Application](#configure-the-inbound-application)
- [Configure the Outbound Application](#configure-the-outbound-application)


### Prerequisites
- A [PingOne Worker application](https://docs.pingidentity.com/r/en-us/pingone/p1_c_applicationtypes#:~:text=application%20%2D%20Single%20page-,Worker,-An%20administrator%20application) that is enabled and the Token Endpoint Authentication Method set to Client Secret Post.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-P1Worker.png?raw=true" width="auto">
  </details>
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

- Wait until you see the following message, then return to your ForgeRock Identity Cloud Platform UI. If you navigate to Identities > Connect, your RCS should now show <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-Connected.png?raw=true" height="30" width="auto">.
```
RCS 1.5.20.15 started.
Press q to shutdown.
```

### Configure the Inbound Application
- In your ForgeRock Identity Cloud Platform UI, navigate to Applications, then click Browse App Catalog.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-AppCatalog.png?raw=true" width="auto">
  </details>

- Search for "Groovy" and select the Scripted Groovy Connector.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-ScriptedGroovy.png?raw=true" width="auto">
  </details>

- Name your connector. Suggestion: "ScriptedPingOne - Inbound"
- Select an Application Owner.
- Check the Authoritative box.
- [Optional] Provide a description and a logo.
- Click Save.
- On the next page, select the Provisioning tab, then click Set up Provisioning.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-SetupProvisioning.png?raw=true" width="auto">
  </details>

- Select your RCS and click Next.
- On the modal, provide the path to your scripts folder on the server. Then, provide the name of each script.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-ConnectorConfiguration.png?raw=true" width="auto">
  </details>

- For Custom Sensitive Configuration, enter the Environment ID, Client ID, and Client Secret from your PingOne Worker application in the following format. \
  ```ENV_ID='<env-id>';CLIENT_ID='<client-id>';CLIENT_SECRET='<client-secret>';``` \
  _Note: This value does not persist in the GUI. To change these values, re-enter the whole string. If security is not a concern, you can use the Custom Configuration field available under Show advanced settings. This field functions similarly but is persisted to the GUI and can quickly and easily be updated._
  <details>
    <summary>Image - Custom Sensitive Configuration</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-CustomSensitiveConfiguration.png?raw=true" width="auto">
  </details>
  <details>
    <summary>Image - Custom Configuration</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-CustomConfiguration.png?raw=true" width="auto">
  </details>

- Click Connect.
- Navigate to the Provisioning tab and select Data. After a brief moment, you should see the users from your PingOne environment populate.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-AccountDataInbound.png?raw=true" width="auto">
  </details>

- Navigate to Mapping > Inbound and map your PingOne attributed to ForgeRock Identity Cloud attributes.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-AccountMappingInbound.png?raw=true" width="auto">
  </details>

- Navigate to Reconciliation > Settings, scroll down to Show advanced settings, check the Persist Associations box, and click Save. This setting can be disabled later but is a useful visualization and troubleshooting tool.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-PersistAssociations.png?raw=true" width="auto">
  </details>

- Navigate to Reconciliation > Reconcile and click Reconcile Now. This will get information from your PingOne users and create them in ForgeRock Identity Cloud based on your Inbound Mapping.
  <details>
    <summary>Image</summary>
    TODO: Capture an image of an initial reconciliation
  </details>

### Configure the Outbound Application
_Note: Many of the steps detailed here are very similar, if not the same, as [Configure the Inbound Application](#configure-the-inbound-application), but not all. Pay close attention._

- In your ForgeRock Identity Cloud Platform UI, navigate to Applications, then click Browse App Catalog.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-AppCatalog.png?raw=true" width="auto">
  </details>

- Search for "Groovy" and select the Scripted Groovy Connector.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-ScriptedGroovy.png?raw=true" width="auto">
  </details>

- Name your connector. Suggestion: "ScriptedPingOne - Outbound"
- Select an Application Owner.
- DO NOT Check the Authoritative box.
- [Optional] Provide a description and a logo.
- Click Create Application.
- On the next page, select the Provisioning tab, then click Set up Provisioning.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-OutboundSetupProvisioning.png?raw=true" width="auto">
  </details>

- Select your RCS and click Next.
- On the modal, provide the path to your scripts folder on the server. Then, provide the name of each script.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-ConnectorConfiguration.png?raw=true" width="auto">
  </details>

- For Custom Sensitive Configuration, enter the Environment ID, Client ID, and Client Secret from your PingOne Worker application in the following format. \
  ```ENV_ID='<env-id>';CLIENT_ID='<client-id>';CLIENT_SECRET='<client-secret>';``` \
  _Note: This value does not persist in the GUI. To change these values, re-enter the whole string. If security is not a concern, you can use the Custom Configuration field available under Show advanced settings. This field functions similarly but is persisted to the GUI and can quickly and easily be updated._
  <details>
    <summary>Image - Custom Sensitive Configuration</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-CustomSensitiveConfiguration.png?raw=true" width="auto">
  </details>
  <details>
    <summary>Image - Custom Configuration</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-CustomConfiguration.png?raw=true" width="auto">
  </details>

- Click Connect.
- Navigate to the Provisioning tab and select Data. After a brief moment, you should see the users from your PingOne environment populate.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-AccountDataInbound.png?raw=true" width="auto">
  </details>

- Navigate to Mapping > Inbound and map your PingOne attributed to ForgeRock Identity Cloud attributes.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-AccountMappingInbound.png?raw=true" width="auto">
  </details>
### KYLE START HERE
- Navigate to Reconciliation > Settings, scroll down to Show advanced settings, check the Persist Associations box, and click Save. This setting can be disabled later but is a useful visualization and troubleshooting tool.
  <details>
    <summary>Image</summary>
    <img src="https://github.com/kylemoorehead-pingidentity/ScriptedPingOne/blob/master/images/FRIC-PersistAssociations.png?raw=true" width="auto">
  </details>

- Navigate to Reconciliation > Reconcile and click Reconcile Now. This will get information from your PingOne users and create them in ForgeRock Identity Cloud based on your Inbound Mapping.
  <details>
    <summary>Image</summary>
    TODO: Capture an image of an initial reconciliation
  </details>
