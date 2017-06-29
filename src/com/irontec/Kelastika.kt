package com.irontec

import khttp.*
import khttp.responses.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.commons.io.IOUtils
import org.json.JSONObject

    var mElasticIndice: String = ""
    var mElasticType: String = ""
    var mFileName: String = ""
    var mHostname: String = "http://localhost"
    var mPort: String = "9200"

    // Tika options
    var TIKA_OPTION_JSON_METADATA: String = "-j"
    var TIKA_OPTION_PLAINTEXT_CONTENT: String = "-T"

    fun main(args: Array<String>) {
        println("Hostname " + mHostname)
        var options = generateOptions()
        var parser = DefaultParser()
        var cmd: CommandLine
        try {
            cmd = parser.parse(options, args)
            readOptions(cmd, options)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        var extractTikaJsonMetadata = String.format("%s %s %s",
                "java -jar tika-app.jar", TIKA_OPTION_JSON_METADATA, mFileName)
        var extractTikaPlainTextContent = String.format("%s %s %s",
                "java -jar tika-app.jar", TIKA_OPTION_PLAINTEXT_CONTENT, mFileName)

        var jsonMetadata: String = ""
        var plainTextContent: String = ""
        try {
            jsonMetadata = executeRuntimeCommand(extractTikaJsonMetadata)
            plainTextContent = executeRuntimeCommand(extractTikaPlainTextContent)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        var jsonObj = JSONObject(jsonMetadata)
        jsonObj.put("content", plainTextContent)

        var elasticResponse: String = ""
        try {
            elasticResponse = postDataToElastic(jsonObj.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var jsonElastic = JSONObject(elasticResponse)
        System.out.println(jsonElastic.toString())
    }

    fun postDataToElastic(data: String): String {

        var elasticEndpointFull = String.format("%s:%s/%s/%s/",
                mHostname, mPort, mElasticIndice, mElasticType)
        var elasticEndpointIndex = String.format("%s:%s/%s/",
                mHostname, mPort, mElasticIndice)
        var elasticEndpointType = String.format("%s:%s/%s/_mapping/%s/",
                mHostname, mPort, mElasticIndice, mElasticType)

        System.out.println(elasticEndpointFull)

        var postData = data.byteInputStream(StandardCharsets.UTF_8).reader().readText();
        var postDataLength = postData.length

        val headers: HashMap<String, String> = hashMapOf(
                "Content-Type" to "application/json",
                "charset" to "utf-8",
                "Content-Length" to Integer.toString(postDataLength))

        // Check index
        val headIndexResponse = check(elasticEndpointIndex)
        if (headIndexResponse.statusCode == 404) {
            val jsonIndexObject = JSONObject("{\"settings\" : {\"index\" : {\"number_of_shards\" : 3,\"number_of_replicas\" : 2}}}")
            val putResponse = create(elasticEndpointIndex, jsonIndexObject)
            if (putResponse.statusCode == 200) {
                val jsonTypeObject = JSONObject("{\"properties\": {\"content\": {\"type\": \"text\"}}}")
                create(elasticEndpointType, jsonTypeObject)
            }
        } else {
            // Check type
            val headTypeResponse = check(elasticEndpointType)
            if (headTypeResponse.statusCode == 404) {
                val jsonTypeObject = JSONObject("{\"properties\": {\"content\": {\"type\": \"text\"}}}")
                create(elasticEndpointType, jsonTypeObject)
            }
        }

        val postResponse = post(elasticEndpointFull, headers, emptyMap(), postData,null,null,null, DEFAULT_TIMEOUT,true,false, emptyList())

        return postResponse.text
    }

    fun create(url: String, json: JSONObject): Response {
        return put(url, emptyMap(), emptyMap(), null, json, null, null, DEFAULT_TIMEOUT, true, false, emptyList())
    }

    fun check(url: String): Response {
        return head(url, emptyMap(), emptyMap(), null, null, null, null, DEFAULT_TIMEOUT, true, false, emptyList())
    }

    fun executeRuntimeCommand(command: String): String {

        println("Executing: " + command)

        var rt = Runtime.getRuntime()
        var proc = rt.exec(command)
        proc.waitFor()

        var stdInput = BufferedReader(InputStreamReader(proc.getInputStream()))

        var stdError = BufferedReader(InputStreamReader(proc.getErrorStream()))

        var error = IOUtils.toString(stdError)
        if (!error.isEmpty()) {
            // We have errors and thus we must exit
            System.out.print(error)
            System.exit(0)
        }

        var result = IOUtils.toString(stdInput)

        return result
    }

    fun generateOptions(): Options {
        var options = Options()

        options.addOption("i", "indice", true, "(Required) Elastic indice name.")

        options.addOption("t", "type", true, "(Required) Elastic indice type name.")

        options.addOption("f", "file", true, "(Required) The document to be parsed and sent to Elastic.")

        options.addOption("h", "host", true, "(Optional) Elastic REST Endpoint hostname. Default http://localhost.")

        options.addOption("p", "port", true, "(Optional) Elastic REST Endpoint port. Default 9200.")

        options.addOption("?", "help", false, "Print this usage message")

        options.addOption("v", "version", false, "Display version information")

        return options
    }

    fun readOptions(cmd: CommandLine, options: Options) {
        if (cmd.hasOption("?") || cmd.hasOption("help")) {
            var formatter = HelpFormatter()
            formatter.printHelp("elastika", options)
            System.exit(0)
        }
        if (cmd.hasOption("v") || cmd.hasOption("version")) {
            System.out.println("Elastika v0.9 by Irontec S.L.")
            System.out.println("Author: Axier Fernandez")
            System.exit(0)
        }
        if (cmd.hasOption("i")) {
            mElasticIndice = cmd.getOptionValue("i")
        }
        if (cmd.hasOption("indice")) {
            mElasticIndice = cmd.getOptionValue("indice")
        }
        if (mElasticIndice.isEmpty()) {
            System.out.print("Missing required parameter Elastic indice. Try executing the program with -i or --indice options and the name of the indice")
            System.exit(0)
        }
        if (cmd.hasOption("t")) {
            mElasticType = cmd.getOptionValue("t")
        }
        if (cmd.hasOption("type")) {
            mElasticType = cmd.getOptionValue("type")
        }
        if (mElasticType.isEmpty()) {
            System.out.print("Missing required parameter Elastic indice type. Try executing the program with -t or --type options and the name of the indice type")
            System.exit(0)
        }
        if (cmd.hasOption("f")) {
            mFileName = cmd.getOptionValue("p")
        }
        if (cmd.hasOption("file")) {
            mFileName = cmd.getOptionValue("file")
        }
        if (mFileName.isEmpty()) {
            System.out.print("Missing required parameter local file name. Try executing the program with -f or --file options and the name of the file")
            System.exit(0)
        }
        if (cmd.hasOption("h")) {
            mHostname = cmd.getOptionValue("h")
        }
        if (cmd.hasOption("host")) {
            mHostname = cmd.getOptionValue("host")
        }
        if (cmd.hasOption("p")) {
            mPort = cmd.getOptionValue("p")
        }
        if (cmd.hasOption("port")) {
            mPort = cmd.getOptionValue("port")
        }
    }

