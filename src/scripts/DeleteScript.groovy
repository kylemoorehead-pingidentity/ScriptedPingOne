@Grapes([
        @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1'),
        @Grab(group='org.slf4j', module='slf4j-api', version='1.6.1'),
        @Grab(group='ch.qos.logback', module='logback-classic', version='0.9.28'),
        @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.13')
])
import groovy.json.JsonSlurper
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
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
println "########## ObjectClass: " + objectClass.objectClassValue
def query = [:]
def queryFilter = 'true'
def get = false
def or = false
def access_token = null

switch(objectClass){
    case ObjectClass.ACCOUNT:
        println "Deleting Account for " + objectClass + ": " + uid.uidValue + " Account"
        CloseableHttpClient httpClient = HttpClients.createDefault();
        def userDelete = null
        access_token = getAccessToken(CLIENT_ID,CLIENT_SECRET,TOKEN_URL)
        //println access_token
        userDelete = new HttpDelete(USER_URL+"/"+uid.uidValue)
        println "Deleting User: "+ USER_URL+"/"+uid.uidValue
        userDelete.addHeader("Authorization", "Bearer ${access_token}")
        def response2 = httpClient.execute(userDelete)
        int statusCode = response2.getStatusLine().getStatusCode();
        println "Status Code: "+statusCode
        //def parsedJson = null
        //parsedJson = new groovy.json.JsonSlurper().parseText(response2.entity.content.text)
        //if(parsedJson){
        return true
        //} else {
        //    println "User not found "+uid.uidValue
        //    return false
        //}
    case ObjectClass.GROUP:
        throw new ConnectorException("Deleting object of type: " + objectClass.objectClassValue + " is not supported")

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