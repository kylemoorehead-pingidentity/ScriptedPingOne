# ScriptedPingOne
**Capabilities**
* Create, Update, Delete, Search of PingOne Accounts
* Grant/ Revoke Group Memberships and Role Memberships

**Limitations**
* Doesn't support schema discovery
* App Template mappings for NAOs have an issue (ignore this if you don't care about ForgeRock Identity Governance)
* Groups are specific to a population. You can't assign a group from a different population to a user in another population
  
**Pre-steps**
1. A PingOne Environment
2. A ForgeRock Identity Cloud tenancy

**Configuration Steps for PingOne as a target application**
1. Create a Worker app with Identity Data Admin privileges
   ![image](https://github.com/srallapally/ScriptedPingOne/assets/84463098/048cb40d-4962-41ed-9dd2-e6ae5695a238)
2. **Ensure that you have selected Client Credentials and Client Secret Post**
   ![image](https://github.com/srallapally/ScriptedPingOne/assets/84463098/31d6d41d-d836-41ea-ba2e-565f995f0196)
3. Use the Postman collection from https://developer.pingidentity.com/en.html and use the Oauth credentials to verify that you are able to search for environments, populations, users, groups and roles. This connector is used **to manage users, roles, groups in one Environment only**
4. Deploy RCS (you can download the Java RCS from backstage.forgerock.com) by unzipping the distribution. I deployed the distribution in ~/Downloads/openicf (RCS_HOME)
5. In the host machine where you deployed RCS, ensure JDK 11 is available in the path. I used openjdk full version "11.0.11+9"
6. Deploy the groovy scripts. I deployed them in RCS_HOME/scriptedpingone
7. Configure RCS to communicate with your Identity Cloud instance in Client mode
    connectorserver.connectorServerName=myldaprcs
    connectorserver.url=wss://<idcloud hostname including domain name>:443/openicf/0
    connectorserver.tokenEndpoint=https://<idcloud hostname including domain name>/am/oauth2/realms/root/realms/alpha/access_token
    connectorserver.clientId=RCSClient
    connectorserver.clientSecret=<client secret from the Identity Cloud UI --> Identities --> Connect --> RCS>
8. Start RCS. I use interactive/ terminal mode so it is easy to debug my scripts.
9. Verify that RCS is able to connect to Identity Cloud. You can verify this from the Identity Cloud  UI (though the RCS terminal window will show you if there are issues)
10. Configure the connectivity information in the scripts. There are better ways than putting creds in the script. E.g. Scripted connectors support sensitive configuration
which allow you to pass creds securely to RCS. You could make use of your own secret store.
11. Search, Update, Create and Delete Scripts are self-explanatory on what needs to be provided, so won't repeat it here. Ok fine. You need the client id, client secret (of the Worker app)
and the environment id (of the environment you want to manage)
12. Create an app using the Scripted Groovy App Template from the App Catalog
    ![image](https://github.com/srallapally/ScriptedPingOne/assets/84463098/c26b3491-f171-43fd-90d5-2240ebafd510)

13. Click the Provisioning tab --> Select the RCS instance --> Provide the connection parameters
    ![image](https://github.com/srallapally/ScriptedPingOne/assets/84463098/9dcfcf8e-3617-40c3-a87e-debf0d57eb2b)

14.  If all goes to plan, the provisioner should be generated
15.  Configure each object type by specifying the attribute to be used as the Display Name
16.  Configure outbound mappings. Mine looks as follows
![image](https://github.com/srallapally/ScriptedPingOne/assets/84463098/bc12a6be-fc01-428a-ba76-de807b25ec67)

17.  Navigate to Users and Roles and assign the app to a user (or a role)

**Optional**
1. To configure PingOne as an authoritative app (or profile master), check the authoritative checkbox while creating the app
2. Define an inbound mapping
3. Configure the correlation query so that you can run authoritative recon and bring in new and modified users
   
**Some more errata**
PingOne APIs use cursor-based paging. The SearchScript passes the cursor in the pagedResultsCookie. The issue you run into is that
the UI doesn't know about this. To fix this, you need to add the following parameters to the inbound mapping (App --> ID Cloud) using PUT
 "reconSourceQueryPaging" : true,
 "reconSourceQueryPageSize" : 20,
 "sourceQueryFullEntry" : true,
 "sourceQuery" : {
      "_queryFilter" : "true"
}
