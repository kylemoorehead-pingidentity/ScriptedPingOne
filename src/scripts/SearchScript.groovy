@Grapes([
        @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1'),
        @Grab(group='org.slf4j', module='slf4j-api', version='1.6.1'),
        @Grab(group='ch.qos.logback', module='logback-classic', version='0.9.28'),
        @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.13')
])
import groovy.json.JsonSlurper
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair


import groovy.transform.Field
import net.sf.json.groovy.JsonSlurper

import org.slf4j.*
import org.apache.http.client.methods.CloseableHttpResponse
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.forgerock.openicf.connectors.groovy.MapFilterVisitor
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.OrFilter
import org.identityconnectors.framework.common.objects.filter.FilterBuilder
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.FrameworkUtil
import static org.identityconnectors.framework.common.objects.AttributeUtil.getAsStringValue;


def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def pageSize = 10
def currentPagedResultsCookie = null

def resultCount = 0

@Field final CLIENT_ID = "__REPLACE_ME__"
@Field final CLIENT_SECRET ="__REPLACE_ME__"
@Field final ENV_ID = "__REPLACE_ME__"
@Field final API_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/"
@Field final TOKEN_URL = "https://auth.pingone.com/${ENV_ID}/as/token"
@Field final USER_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/users"
@Field final GROUP_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/groups"
@Field final POPULATION_URL = "https://api.pingone.com/v1/environments/${ENV_ID}/populations"
@Field final ROLE_URL = "https://api.pingone.com/v1/roles"

//println "########## Entering " + operation + " Script for " + objectClass
//println "########## ObjectClass: " + objectClass.objectClassValue
def query = [:]
def queryFilter = 'true'
def get = false
def or = false
def access_token = null
switch (objectClass.objectClassValue) {
    case '__ACCOUNT__':
        def userUrl = null
        def attrName = null
        if(filter != null){
            if(filter instanceof EqualsFilter){
                attrName = ((EqualsFilter) filter).getAttribute()
                //println "Attribute Name: ${attrName}"
                if (attrName.is(Uid.NAME)) {
                    def username = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                    userUrl = USER_URL + "?filter=id%20eq%20%22${username}%22"
                } else if(attrName.is(Name.NAME)){
                    def username = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                    userUrl = USER_URL + "?filter=username%20eq%20%22${username}%22"
                }
                else {
                    def attrValue = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                    attrName = ((EqualsFilter) filter).getAttribute().getName()
                    //bug in code, userName is not a valid attribute
                    if(attrName == "userName"){
                        attrName = "username"
                    }
                    userUrl = USER_URL + "?filter=${attrName}%20eq%20%22${attrValue}%22"
                    //println "User URL: ${userUrl}"
                }
            } else if (filter instanceof OrFilter){
                println "#### OrFilter ####"
                def keys = getOrFilters((OrFilter)filter)
                println "#### keys ####" + keys
                def s = null
                keys.each { key ->
                    if(s) {
                        s = s + "%20or%20id%20eq%20%22"+key+"%22"
                    } else {
                        s = "id%20eq%20%22"+key+"%22"
                    }
                }
                userUrl = USER_URL + "?filter=(${s})"
                println "User URL: ${userUrl}"
            }
            CloseableHttpClient httpClient = HttpClients.createDefault();
            def userGet = null
            access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
            //println access_token
            userGet = new HttpGet(userUrl)
            userGet.addHeader("Authorization", "Bearer ${access_token}")
            def response2 = httpClient.execute(userGet)
            def parsedJson = new groovy.json.JsonSlurper().parseText(response2.entity.content.text)
            //println "Parsed JSON: ${parsedJson}"
            parsedJson._embedded.users.each { user ->
                def groupMemberships = []
                def roleAssignments = []
                def lastSignOn = null
                def lastSignOnAt = null

                if(user.lastSignOn){
                    lastSignOn = user.lastSignOn
                    lastSignOnAt = lastSignOn.at
                }

                def groupMembershipUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${user.id}/memberOfGroups?expand=group&limit=100&filter=type%20eq%20%22DIRECT%22"
                def roleMembershipUrl  = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${user.id}/roleAssignments"
                def populationMembershipUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users//${user.id}/population"
                def populationMemberships = []
                def getGrps = new HttpGet(groupMembershipUrl)
                getGrps.addHeader("Authorization", "Bearer ${access_token}")
                def Grpresponse = httpClient.execute(getGrps)
                def parsedGrps = null
                parsedGrps = new JsonSlurper().parseText(Grpresponse.entity.content.text)
                if(parsedGrps){
                    parsedGrps._embedded.groupMemberships.each { membership ->
                        groupMemberships.add(membership._embedded.group.id)
                    }
                }

                def getRoles = new HttpGet(roleMembershipUrl)
                getRoles.addHeader("Authorization", "Bearer ${access_token}")
                def RoleResponse = httpClient.execute(getRoles)
                def parsedRoles = null
                parsedRoles = new JsonSlurper().parseText(RoleResponse.entity.content.text)
                if(parsedRoles){
                    parsedRoles._embedded.roleAssignments.each { assignment ->
                        //println "Role ID: ${assignment.role.id}"
                        roleAssignments.add(assignment.role.id)
                    }
                }

                handler {
                    uid user.id
                    id user.username
                    attribute 'givenName', user.name.given
                    attribute 'sn', user.name.family
                    attribute 'email', user.email
                    attribute 'honorificPrefix', user.name.honorificPrefix
                    attribute 'honorificSuffix', user.name.honorificSuffix
                    attribute 'formattedName', user.name.formatted
                    attribute 'environmentid', user.environment.id
                    attribute 'populationid', user.population.id
                    attribute 'canAuthenticate', user.account.canAuthenticate
                    attribute 'accountStatus', user.account.status
                    attribute 'enabled', user.enabled
                    attribute 'identityProviderType', user.identityProvider.type
                    attribute 'lastSignOn', lastSignOnAt
                    attribute 'lifecycleStatus', user.lifecycle.status
                    attribute 'mfaEnabled', user.mfaEnabled
                    attribute 'createdAt', user.createdAt
                    attribute 'updatedAt', user.updatedAt
                    attribute 'verifyStatus', user.verifyStatus
                    attribute 'groupMemberships', groupMemberships
                    attribute 'roleAssignments', roleAssignments
                }
            }
            return new SearchResult()
        } else if(filter == null){

            if (null != options.getPageSize() && options.getPageSize() > 0) {
                pageSize = options.getPageSize()
            }
            if(null != options.getPagedResultsOffset() && options.getPagedResultsOffset() > 0){
                println "Page Offset = " + options.getPagedResultsOffset()
            }
            userUrl = USER_URL+"?limit="+pageSize

            if (null != options.pagedResultsCookie) {
                currentPagedResultsCookie = options.pagedResultsCookie.toString()
                //if(currentPagedResultsCookie){
                //    println " Found a cursor"
                //}
                //println "paged results cookie "+currentPagedResultsCookie
                userUrl = userUrl + "&cursor="+currentPagedResultsCookie
            } else {
                println "No paged results cookie"
            }
            //println "No filter. User URL: ${userUrl}"
            CloseableHttpClient httpClient = HttpClients.createDefault();
            def userGet = null
            access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
            //println access_token
            userGet = new HttpGet(userUrl)
            userGet.addHeader("Authorization", "Bearer ${access_token}")
            def response2 = httpClient.execute(userGet)
            def parsedJson = new groovy.json.JsonSlurper().parseText(response2.entity.content.text)
            if(!parsedJson){
                println "No users found"
                return new SearchResult()
            } else {
                def nextUrl = null
                try {
                    if( parsedJson._links.next.href){
                        nextUrl = parsedJson._links.next.href
                    } else {
                        println "No more rows"
                    }
                } catch(Exception e){
                    nextUrl = null
                }
                //println "nextUrl: ${nextUrl}"
                if(nextUrl){
                    def nextUrlSplit = nextUrl.split("cursor=")
                    currentPagedResultsCookie = nextUrlSplit[1]
                    //println "currentPagedResultsCookie: ${currentPagedResultsCookie}"
                }
                try {
                    parsedJson._embedded.users.each { user ->
                        def groupMemberships = []
                        def roleAssignments = []
                        def lastSignOn = null
                        def lastSignOnAt = null

                        if(user.lastSignOn){
                            lastSignOn = user.lastSignOn
                            lastSignOnAt = lastSignOn.at
                        }

                        def groupMembershipUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${user.id}/memberOfGroups?expand=group&limit=100&filter=type%20eq%20%22DIRECT%22"
                        def roleMembershipUrl  = "https://api.pingone.com/v1/environments/${ENV_ID}/users/${user.id}/roleAssignments"
                        def populationMembershipUrl = "https://api.pingone.com/v1/environments/${ENV_ID}/users//${user.id}/population"
                        //println "Population Membership URL: ${populationMembershipUrl}"
                        def populationMemberships = []

                        def getGrps = new HttpGet(groupMembershipUrl)
                        getGrps.addHeader("Authorization", "Bearer ${access_token}")
                        def Grpresponse = httpClient.execute(getGrps)
                        def parsedGrps = null
                        parsedGrps = new JsonSlurper().parseText(Grpresponse.entity.content.text)
                        if(parsedGrps){
                            parsedGrps._embedded.groupMemberships.each { membership ->
                                groupMemberships.add(membership._embedded.group.id)
                            }
                        }

                        def getRoles = new HttpGet(roleMembershipUrl)
                        getRoles.addHeader("Authorization", "Bearer ${access_token}")
                        def RoleResponse = httpClient.execute(getRoles)
                        def parsedRoles = null
                        parsedRoles = new JsonSlurper().parseText(RoleResponse.entity.content.text)
                        if(parsedRoles){
                            parsedRoles._embedded.roleAssignments.each { assignment ->
                                //println "Role ID: ${assignment.role.id}"
                                roleAssignments.add(assignment.role.id)
                            }
                        }

                        handler {
                            uid user.id
                            id user.username
                            attribute 'givenName', user.name.given
                            attribute 'sn', user.name.family
                            attribute 'email', user.email
                            attribute 'honorificPrefix', user.name.honorificPrefix
                            attribute 'honorificSuffix', user.name.honorificSuffix
                            attribute 'formattedName', user.name.formatted
                            attribute 'environmentid', user.environment.id
                            attribute 'populationid', user.population.id
                            attribute 'canAuthenticate', user.account.canAuthenticate
                            attribute 'accountStatus', user.account.status
                            attribute 'enabled', user.enabled
                            attribute 'identityProviderType', user.identityProvider.type
                            attribute 'lastSignOn', lastSignOnAt
                            attribute 'lifecycleStatus', user.lifecycle.status
                            attribute 'mfaEnabled', user.mfaEnabled
                            attribute 'createdAt', user.createdAt
                            attribute 'updatedAt', user.updatedAt
                            attribute 'verifyStatus', user.verifyStatus
                            attribute 'groupMemberships', groupMemberships
                            attribute 'roleAssignments', roleAssignments
                        }
                    }
                } catch (Exception e){println "Exception: ${e}"}
                if(nextUrl == null) {
                    println "No more users"
                    return new SearchResult()
                } else {
                    return new SearchResult(currentPagedResultsCookie.toString(),-1)
                }
            }
        }
    case '__GROUP__':
        def groupUrl = null
        def attrName = null
        if(filter == null){
            groupUrl = GROUP_URL
        } else if(filter instanceof EqualsFilter){
            attrName = ((EqualsFilter) filter).getAttribute()
            if (attrName.is(Uid.NAME) || attrName.is(Name.NAME)) {
                def grpname = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                groupUrl = GROUP_URL + "?filter=id%20eq%20%22${grpname}%22"
            } else {
                def attrValue = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                attrName = ((EqualsFilter) filter).getAttribute().getName()
                groupUrl = GROUP_URL + "?filter=${attrName}%20eq%20%22${attrValue}%22"
            }
        } else if (filter instanceof OrFilter){
            println "OrFilter is not supported"
            break
        }
        //println "Group URL: ${groupUrl}"
        CloseableHttpClient httpClient = HttpClients.createDefault();
        def groupGet = null
        groupGet = new HttpGet(groupUrl)
        def group_access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
        //println group_access_token
        groupGet.addHeader("Authorization", "Bearer ${group_access_token}")
        def GroupResponse = httpClient.execute(groupGet)
        def parsedGroupJson = null
        parsedGroupJson = new groovy.json.JsonSlurper().parseText(GroupResponse.entity.content.text)

        if(parsedGroupJson){
            parsedGroupJson._embedded.groups.each { group ->
                //println "Group ID: ${group.id}, Group Name: ${group.name}, Group Description: ${group.description}, Direct User Count: ${group.directMemberCounts.users}, Direct Group Count: ${group.directMemberCounts.groups}"

                handler {
                    uid group.id
                    id group.name
                    attribute 'groupId', group.id
                    attribute 'groupName', group.name
                    attribute 'groupDescription', group.description
                    attribute 'environmentId', group.environment.id
                    attribute 'populationId', null
                    attribute 'directUserCount', group.directMemberCounts.users
                    attribute 'directGroupCount', group.directMemberCounts.groups
                }

            }
        }
        return new SearchResult()
    case 'Population':
        println "########## Population ##########"
        CloseableHttpClient httpClient = HttpClients.createDefault();
        def populationGet = null
        def pop_access_token = null
        pop_access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
        def popUrl = null
        def popResponse = null
        def popParsedJson = null
        def ppId  = null
        boolean defaultAttr = false
        if(filter != null){
            if(filter instanceof EqualsFilter){
                attrName = ((EqualsFilter) filter).getAttribute()
                def popName = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                popUrl =POPULATION_URL + "/${popName}"
                println "Population URL: ${popUrl}"
                populationGet = new HttpGet(popUrl)
                //println pop_access_token
                populationGet.addHeader("Authorization", "Bearer ${pop_access_token}")
                popResponse = httpClient.execute(populationGet)
                popParsedJson = new groovy.json.JsonSlurper().parseText(popResponse.entity.content.text)
                println "Parsed JSON: ${popParsedJson}"
                if(popParsedJson){
                    if(popParsedJson.passwordPolicy){
                        ppId = popParsedJson.passwordPolicy.id
                    }
                    defaultAttr = popParsedJson.default
                    println "Default: ${defaultAttr} for ${popParsedJson.name}"
                    try {
                        handler {
                            uid popParsedJson.id
                            id popParsedJson.name
                            attribute 'populationId', popParsedJson.id
                            attribute 'populationName', popParsedJson.name
                            attribute 'description', "description"
                            attribute 'environmentId', popParsedJson.environment.id
                            attribute 'passwordPolicyId', ppId
                            attribute 'userCount', popParsedJson.userCount
                            attribute 'isDefault', defaultAttr
                            attribute 'createdAt', popParsedJson.createdAt
                            attribute 'updatedAt', popParsedJson.updatedAt
                        }
                    } catch (Exception e){
                        println "Exception getting Population details: ${e}"
                    }
                }
            } else if (filter instanceof OrFilter){
                println "OrFilter is not supported"
                break
            }
            return new SearchResult()
        } else {
            populationGet = new HttpGet(POPULATION_URL)
            populationGet.addHeader("Authorization", "Bearer ${pop_access_token}")
            popResponse = httpClient.execute(populationGet)
            popParsedJson = new groovy.json.JsonSlurper().parseText(popResponse.entity.content.text)
            println "Parsed JSON: ${popParsedJson}"
            println "---------------------"
            try {
                if(popParsedJson){
                    popParsedJson._embedded.populations.each { population ->
                        if(population.passwordPolicy){
                            ppId = population.passwordPolicy.id
                        }
                        defaultAttr = false
                        defaultAttr = popParsedJson.default
                        //println "Default: ${defaultAttr}"
                        if(population.default == true){
                            defaultAttr = "true"
                        } else {
                            defaultAttr = "false"
                        }
                        if(defaultAttr == null){
                            defaultAttr = "false"
                        }
                        handler {
                            uid population.id
                            id population.name
                            attribute 'populationId', population.id
                            attribute 'populationName', population.name
                            attribute 'description',"description"
                            attribute 'environmentId', population.environment.id
                            attribute 'passwordPolicyId', ppId
                            attribute 'userCount', population.userCount
                            attribute 'isDefault', defaultAttr
                            attribute 'createdAt', population.createdAt
                            attribute 'updatedAt', population.updatedAt
                        }
                    }
                }
            } catch (Exception e){
                println "Exception getting Population details: ${e}"
            }
            return new SearchResult()
        }
    case 'Role':
        def roleUrl = null
        def roleGet = null
        def attrName = null
        def role_access_token = null
        def RoleResponse = null
        def parsedRoleJson = null
        def applicableTo = []
        role_access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
        if(filter != null){
            if(filter instanceof EqualsFilter){
                attrName = ((EqualsFilter) filter).getAttribute()
                def rolename = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                roleUrl = ROLE_URL + "/${rolename}"
                //println "Role URL: ${roleUrl}"
                CloseableHttpClient httpClient = HttpClients.createDefault();
                roleGet = new HttpGet(roleUrl)
                //println access_token
                roleGet.addHeader("Authorization", "Bearer ${role_access_token}")
                RoleResponse = httpClient.execute(roleGet)
                parsedRoleJson = new groovy.json.JsonSlurper().parseText(RoleResponse.entity.content.text)
                if(parsedRoleJson){
                    handler {
                        uid parsedRoleJson.id
                        id parsedRoleJson.name
                        attribute 'roleId',parsedRoleJson.id
                        attribute 'roleName',parsedRoleJson.name
                        attribute 'description',parsedRoleJson.description
                        if(parsedRoleJson.applicableTo){
                            parsedRoleJson.applicableTo.each { applicable ->
                                applicableTo.add(applicable)
                            }
                        }
                        attribute 'applicableTo', applicableTo
                    }
                }
            } else if (filter instanceof OrFilter){
                println "OrFilter is not supported"
                break
            }
        } else {
            roleUrl = ROLE_URL
            println "Role URL: ${roleUrl}"
            CloseableHttpClient httpClient = HttpClients.createDefault();
            roleGet = new HttpGet(roleUrl)
            //println access_token
            roleGet.addHeader("Authorization", "Bearer ${role_access_token}")
            RoleResponse = httpClient.execute(roleGet)
            parsedRoleJson = new groovy.json.JsonSlurper().parseText(RoleResponse.entity.content.text)
            // println "Parsed Role JSON: ${parsedRoleJson}"
            if(parsedRoleJson){
                parsedRoleJson._embedded.roles.each { role ->
                    handler {
                        uid role.id
                        id role.name
                        attribute 'roleId', role.id
                        attribute 'roleName', role.name
                        attribute 'description', role.description
                        if(role.applicableTo){
                            role.applicableTo.each { applicable ->
                                applicableTo.add(applicable)
                            }
                        }
                        attribute 'applicableTo', applicableTo
                    }

                }
            }
            return new SearchResult()
        }
    default:
        break
}

def getOrFilters(OrFilter filter) {
    def ids = []
    Filter left = filter.getLeft()
    Filter right = filter.getRight()
    if(left instanceof EqualsFilter) {
        String id = ((EqualsFilter)left).getAttribute().getValue().get(0).toString()
        //println "Left ID: ${id}"
        ids.add(id)
    } else if(left instanceof OrFilter) {
        ids.addAll(getOrFilters((OrFilter)left))
    }
    if(right instanceof EqualsFilter) {
        String id = ((EqualsFilter)right).getAttribute().getValue().get(0).toString()
        //println "Right ID: ${id}"
        ids.add(id)
    } else if(right instanceof OrFilter) {
        ids.addAll(getOrFilters((OrFilter)right))
    }
    return ids

}

def getAccessToken(String clientId,String clientSecret,String tokenUrl){
    CloseableHttpClient http =  HttpClients.createDefault();
    def post = new HttpPost(tokenUrl)
    post.addHeader("Content-Type", "application/x-www-form-urlencoded")
    post.entity = new UrlEncodedFormEntity([
            new BasicNameValuePair("grant_type", "client_credentials"),
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_secret", clientSecret)
    ] as List)
    def response = http.execute(post)
    def data = new groovy.json.JsonSlurper().parseText(response.entity.content.text)
    def access_token = data.access_token
    return access_token
}