import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder
import org.identityconnectors.framework.spi.operations.SearchOp

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.REQUIRED

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def log = log as Log

return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attributes {
            id String.class, REQUIRED
            userName String.class, REQUIRED
            givenName String.class, REQUIRED
            sn String.class, REQUIRED
            email String.class, REQUIRED
            honorificSuffix String.class
            honorificPrefix String.class
            formattedName String.class
            environmentid String.class, REQUIRED
            populationid String.class, REQUIRED
            accountStatus String.class
            enabled String.class
            identityProviderType String.class
            lastSignOn String.class
            lifecycleStatus String.class
            canAuthenticate String.class
            mfaEnabled String.class
            createdAt String.class
            updatedAt String.class
            verifyStatus String.class
            groupMemberships String.class, MULTIVALUED
            roleAssignments String.class, MULTIVALUED
        }

    }
    objectClass {
        type ObjectClass.GROUP_NAME
        attributes {
            groupId String.class, REQUIRED
            groupName String.class, REQUIRED
            groupDescription String.class
            environmentId String.class
            populationId String.class
            directUserCount String.class
            directGroupCount String.class
        }
    }

    objectClass {
        type 'Population'
        attributes {
            populationId String.class, REQUIRED
            populationName String.class, REQUIRED
            description String.class
            environmentId String.class
            passwordPolicyId String.class
            userCount String.class
            isDefault String.class
            createdAt String.class
            updatedAt String.class
        }
    }

    objectClass {
        type 'Role'
        attributes {
            roleId String.class, REQUIRED
            roleName String.class, REQUIRED
            description String.class
            applicableTo String.class, MULTIVALUED
        }
    }

    defineOperationOption OperationOptionInfoBuilder.buildPagedResultsCookie(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildPageSize(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildSortKeys(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildRunWithUser()
    defineOperationOption OperationOptionInfoBuilder.buildRunWithPassword()
}
)

