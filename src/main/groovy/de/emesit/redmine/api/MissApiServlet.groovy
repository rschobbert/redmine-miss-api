package de.emesit.redmine.api

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import static javax.servlet.http.HttpServletResponse.SC_OK

import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * <p>
 * A servlet which can be used to serve information on redmine custom fields or enumerations.
 * At the time of writing there is no built-in possibility in redmine to query
 * the configured custom fields or enumerations. To build a REST API I needed this possibility 
 * and therefore wrote this servlet.
 * </p>
 * <p>
 * As soon as redmine has that functionality out-of-the-box, this servlet will
 * become obsolete.
 * </p>
 * <p>
 * To configure this servlet use the following init-parameter names:
 * </p>
 * <ul>
 *   <li>redmineDatabase.&lt;INDEX&gt;.name</li>
 *   <li>redmineDatabase.&lt;INDEX&gt;.jdbcUrl</li>
 *   <li>redmineDatabase.&lt;INDEX&gt;.jdbcUsername</li>
 *   <li>redmineDatabase.&lt;INDEX&gt;.jdbcPassword</li>
 *   <li>redmineDatabase.&lt;INDEX&gt;.jdbcDriver</li>
 * </ul>
 * where &lt;INDEX&gt; is an index usually starting at 0.
 * <p>
 * So an example configuration of this servlet, in case you have two redmine instances running (one
 * production and one development), may look like the following:
 * <pre>
 *     &lt;servlet&gt;
 *      &lt;servlet-name&gt;MissApiServlet&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;de.emesit.redmine.customfield.MissApiServlet&lt;/servlet-class&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.0.name&lt;/param-name&gt;
 *          &lt;param-value&gt;production&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.0.jdbcUrl&lt;/param-name&gt;
 *          &lt;param-value&gt;jdbc:mysql://localhost/redmine-production&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.0.jdbcUsername&lt;/param-name&gt;
 *          &lt;param-value&gt;myuser&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.0.jdbcPassword&lt;/param-name&gt;
 *          &lt;param-value&gt;mypassword&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.0.jdbcDriver&lt;/param-name&gt;
 *          &lt;param-value&gt;com.mysql.jdbc.Driver&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.1.name&lt;/param-name&gt;
 *          &lt;param-value&gt;development&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.1.jdbcUrl&lt;/param-name&gt;
 *          &lt;param-value&gt;jdbc:mysql://localhost/redmine-development&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.1.jdbcUsername&lt;/param-name&gt;
 *          &lt;param-value&gt;myuser&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.1.jdbcPassword&lt;/param-name&gt;
 *          &lt;param-value&gt;mypassword&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;redmineDatabase.1.jdbcDriver&lt;/param-name&gt;
 *          &lt;param-value&gt;com.mysql.jdbc.Driver&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *  &lt;/servlet&gt;
 *  </pre>
 * 
 * @author Ruediger Schobbert
 */
class MissApiServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(MissApiServlet.class)
    
    private static final String PARAM_LOCATION = 'location'
    private static final String PARAM_RELOAD   = 'reload'
    private static final String PARAM_KEY      = 'key'
    private static final String PARAM_TYPE     = 'type'
    
    private static final String RESPONSE_MIME_TYPE = 'application/xml'
    
    List<RedmineDatabase> redmineDatabases = []
    
    @Override
    void init(ServletConfig config) throws ServletException {
        List<Integer> redmineInstanceIndices = extractRedmineInstanceIndices(config.getInitParameterNames().toList())
        LOG.debug("Extracted redmineInstanceIndices: $redmineInstanceIndices")
        
        for (Integer nextIndex : redmineInstanceIndices) {
            String name         = config.getInitParameter("redmineDatabase.${nextIndex}.name")
            String jdbcUrl      = config.getInitParameter("redmineDatabase.${nextIndex}.jdbcUrl")
            String jdbcUsername = config.getInitParameter("redmineDatabase.${nextIndex}.jdbcUsername")
            String jdbcPassword = config.getInitParameter("redmineDatabase.${nextIndex}.jdbcPassword")
            String jdbcDriver   = config.getInitParameter("redmineDatabase.${nextIndex}.jdbcDriver")
            if (name && jdbcUrl && jdbcUsername && jdbcPassword && jdbcDriver) {
                redmineDatabases << new RedmineDatabase(name:name, jdbcUrl:jdbcUrl, jdbcUsername:jdbcUsername, jdbcPassword:jdbcPassword, jdbcDriver:jdbcDriver)
                LOG.debug("Added redmineInstance '$name' with the following parameters: jdbcUrl=$jdbcUrl, jdbcUsername=$jdbcUsername, jdbcDriver=$jdbcDriver")
            } else {
                throw new ServletException("invalid redmineDatabase init parameters at index $nextIndex - one or more are missing: name=$name, jdbcUrl=$jdbcUrl, jdbcUsername=$jdbcUsername, jdbcDriver=$jdbcDriver, "+(!jdbcPassword?', jdbcPassword is null or empty':''))
            }
        }
        LOG.info("Initialized MissApiServlet with $redmineDatabases.size() redmineDatabases: $redmineDatabases")
    }
    
    @Override
    void destroy() {
        super.destroy()
    }
    
    
    
    @Override
    void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.debug "Received request for pathInfo=${request.pathInfo}"
        
        String location = request.getParameter(PARAM_LOCATION)
        boolean reload = request.getParameter(PARAM_RELOAD) != null
        if (location) {
            RedmineDatabase redmineDatabase = redmineDatabases.find { it.name == location }
            LOG.debug "Found redmineDatabase with name $location in $redmineDatabases:  $redmineDatabase"
            if (redmineDatabase) {
                String apiKey = request.getParameter(PARAM_KEY)
                LOG.debug "Checking if apiKey $apiKey exists..."
                boolean apiKeyExists = redmineDatabase.apiKeyExists(apiKey)
                LOG.debug "apiKeyExists = $apiKeyExists"
                if (apiKeyExists) {
                    String typeParam = request.getParameter(PARAM_TYPE)
                    
                    String xmlAnswer = ''
                    if (request.pathInfo == '/custom_fields') {
                        LOG.debug "Querying ${request.pathInfo} in redmineDatabase $redmineDatabase"
                        List<CustomField> customFieldsOfType = redmineDatabase.getCustomFields(typeParam, reload)
                        xmlAnswer = CustomField.toXml(customFieldsOfType)
                    } else if (request.pathInfo == '/enumerations') {
                        LOG.debug "Querying ${request.pathInfo} in redmineDatabase $redmineDatabase"
                        List<Enumeration> enumerationsOfType = redmineDatabase.getEnumerations(typeParam, reload)
                        xmlAnswer = Enumeration.toXml(enumerationsOfType)
                    }
                    LOG.debug "xmlAnswer = $xmlAnswer"
                    response.contentType = RESPONSE_MIME_TYPE
                    response.outputStream << xmlAnswer
                    response.status = SC_OK
                } else {
                    response.status = SC_UNAUTHORIZED
                }
            } else {
                response.status = SC_NOT_FOUND;
            }
        } else {
            response.status = SC_NOT_FOUND;
        }
    }
    
    protected static List<Integer> extractRedmineInstanceIndices(List<String> initParameterNames) {
        Set<Integer> redmineInstanceIndices = [] as Set
        LOG.debug("Found initParameterNames: $initParameterNames")
        initParameterNames.each {
            List paramTokenized = it.tokenize('.')
            LOG.trace("Tokenized next initParameterName '$it' on '.': $paramTokenized")
            if (paramTokenized && paramTokenized.size() == 3) {
                try {
                    LOG.debug("Trying to convert token '${paramTokenized[1]}' to Integer")
                    redmineInstanceIndices << (paramTokenized[1] as Integer)
                    LOG.trace("Conversion to Integer was successful, redmineInstanceIndices is now: $redmineInstanceIndices")
                } catch (Exception exc) {
                    LOG.debug("This is not necessarily an error: Got exception while converting '${paramTokenized[1]}' to an Integer", exc)
                }
            }
        }
        redmineInstanceIndices.toList().sort()
    }
}
