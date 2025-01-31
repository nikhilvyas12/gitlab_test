package de.telekom.k8s_operator.cmds;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.telekom.gac.annotation.Command;
import de.telekom.gac.annotation.Signature;
import de.telekom.k8s_operator.K8sOperatorAgent;
import de.telekom.k8s_operator.model.WatcherContext;
import de.telekom.k8s_operator.services.WatcherService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @GacHelp getRegisteredAgentList Command
 * can be used to Get the Active Agent List which is registered in k8sOperatorAgent and active  ,
 **/

@Slf4j
@Command(agent = K8sOperatorAgent.class)
public class getNotRegisteredAgentList {
    private final WatcherService watcherService;
    public getNotRegisteredAgentList(WatcherService watcherService) {
        this.watcherService = watcherService;
    }

   @Data
    @NoArgsConstructor
    public static class Request {
        private String namespace;
    }

    @Data
    @NoArgsConstructor
    public static class Response {
         List<String> responseList;
    }

    @Signature(inputParameters = {Request.class}, outputParameters = {Response.class})
    public  Response exec(Request request) {
        List<String> localPodNames = List.of("pod1", "pod2", "pod3", "pod4");
        List<String> missingElements =null;
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            // Fetch all pods in the specified namespace
            List<String>  k8sPodNames = client.pods()
                    .inNamespace(request.getNamespace())
                    .list()
                    .getItems()
                    .stream()
                     .map(pod -> {
                         if (pod.getMetadata() != null && pod.getMetadata().getLabels() != null) {
                             return pod.getMetadata().getLabels().get("app"); // Extract the 'app' label
                         }
                         return null; // Return null if metadata or labels are missing
                     })
                     .filter(Objects::nonNull) // Filter out null values
                     .toList();
         //   List<WatcherContext> watcherList = extractJson();
            List<String> mockAgentNames = getMockAgentNames();
            // Use a HashSet for faster lookups
            HashSet<String> setA = new HashSet<>(k8sPodNames);
            missingElements = new ArrayList<>();

            // Find elements in B that are not in A
            for (String item : mockAgentNames) {
                if (!setA.contains(item)) {
                    missingElements.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching pod information: " + e.getMessage());
            e.printStackTrace();
        }

        Response response = new Response();
        response.setResponseList(missingElements);
        return response;
    }


    private static String extractAppLabelFromPod(Pod pod) {
        if (pod == null || pod.getMetadata() == null || pod.getMetadata().getLabels() == null) {
            return null;
        }

        Map<String, String> labels = pod.getMetadata().getLabels();
        return labels.get("app"); // Return the 'app' label
    }

    public static List<String> getMockAgentNames() {
        return new ArrayList<>(Arrays.asList(
                "bng-agent-qa",
                "k8s-agent-qa",
                "demo-gac-agent-big",
                "demo-gac-agent-med",
                "database-agent-qa",
                "generic-resource-agent-qa",
                "telemetry-agent-qa",
                "file-agent-qa",
                "rest-mockup-agent-qa",
                "demo-gac-agent-dev",
                "demo-gac-agent-small",
                "lims-rax-agent-qa",
                "slimbot-agent-qa",
                "kafka-agent-qa",
                "mail-client-agent-qa"
        ));
    }


    public static List<String> findMissingElements(List<String> listA, List<String> listB) {
        // Use a HashSet for faster lookups
        HashSet<String> setA = new HashSet<>(listA);
        List<String> missingElements = new ArrayList<>();

        // Find elements in B that are not in A
        for (String item : listB) {
            if (!setA.contains(item)) {
                missingElements.add(item);
            }
        }

        return missingElements;
    }

    private List<WatcherContext> extractJson() {
        List<WatcherContext> watcherList = null;

        try {
            // Create Gson instance
            Gson gson = new Gson();

            // Specify the file path
            File jsonFile = new File("/Users/A200224442/Documents/Projects/data/NotRegisterList/watched.json");
            if (!jsonFile.exists()) {
                System.err.println("File not found at: " + jsonFile.getAbsolutePath());
                return null;
            }

            // Read JSON file into a JsonObject
            FileReader reader = new FileReader(jsonFile);
            JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();

            // Validate "Response" object
            if (!rootObject.has("Response") || rootObject.get("Response").isJsonNull()) {
                System.err.println("'Response' object is missing or null in JSON.");
                return null;
            }

            JsonObject responseObject = rootObject.getAsJsonObject("Response");

            // Validate "responseList" array
            if (!responseObject.has("responseList") || responseObject.get("responseList").isJsonNull()) {
                System.err.println("'responseList' array is missing or null in JSON.");
                return null;
            }

            // Get "responseList" as JSON array string
            String responseListJson = responseObject.getAsJsonArray("responseList").toString();

            // Define the target list type
            Type listType = new TypeToken<List<WatcherContext>>() {
            }.getType();

            // Convert JSON array string to List<WatcherContext>
            watcherList = gson.fromJson(responseListJson, listType);

            // Print the result
            watcherList.forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return watcherList;
    }
}


