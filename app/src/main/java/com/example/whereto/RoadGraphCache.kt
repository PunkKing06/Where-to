package com.example.whereto

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

/**
 * Small, private on-device store for road graphs. It deliberately lives on
 * the phone rather than a server: the user only downloads a neighbourhood
 * once, and the app can continue to create drives there while offline.
 */
object RoadGraphCache {
    private const val MAX_FILES = 8
    private const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

    private fun directory(context: Context): File = File(context.filesDir, "road-graphs").also {
        if (!it.exists()) it.mkdirs()
    }

    fun loadBest(context: Context, center: LatLng, requestedRadiusMeters: Int): RoadGraph? {
        val now = System.currentTimeMillis()
        var best: RoadGraph? = null
        var bestDistance = Double.MAX_VALUE

        directory(context).listFiles { file -> file.extension == "json" }?.forEach { file ->
            try {
                if (now - file.lastModified() > MAX_AGE_MS) return@forEach
                val graph = decode(file.readText()) ?: return@forEach
                // A saved circle must be at least as large as the requested
                // one, and the current location must remain comfortably
                // inside it so there are roads in every direction.
                val distance = distanceMetersBetween(center, graph.fetchCenter)
                if (graph.fetchRadiusMeters >= requestedRadiusMeters &&
                    distance <= graph.fetchRadiusMeters * 0.35 && distance < bestDistance
                ) {
                    best = graph
                    bestDistance = distance
                }
            } catch (_: Exception) {
                // A partial/corrupt cache file is ignored; a later successful
                // road lookup will replace it.
            }
        }
        return best
    }

    fun save(context: Context, graph: RoadGraph) {
        try {
            val dir = directory(context)
            val file = File(dir, fileName(graph.fetchCenter))
            val temp = File(dir, "${file.name}.new")
            temp.writeText(encode(graph).toString())
            if (file.exists()) file.delete()
            temp.renameTo(file)
            trim(dir)
        } catch (_: Exception) {
            // The live graph is still usable if the device cannot cache it.
        }
    }

    private fun fileName(center: LatLng): String {
        val lat = (center.latitude * 100).roundToInt()
        val lon = (center.longitude * 100).roundToInt()
        return "${lat}_${lon}.json"
    }

    private fun trim(dir: File) {
        dir.listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.lastModified() }
            ?.dropLast(MAX_FILES)
            ?.forEach { it.delete() }
    }

    private fun encode(graph: RoadGraph): JSONObject = JSONObject().apply {
        put("latitude", graph.fetchCenter.latitude)
        put("longitude", graph.fetchCenter.longitude)
        put("radiusMeters", graph.fetchRadiusMeters)
        put("nodes", JSONArray().also { nodes ->
            graph.nodePositions.forEach { (id, point) ->
                nodes.put(JSONArray().put(id).put(point.latitude).put(point.longitude))
            }
        })
        put("edges", JSONArray().also { edges ->
            graph.adjacency.forEach { (from, destinations) ->
                destinations.forEach { to ->
                    edges.put(JSONArray().put(from).put(to).put(graph.streetNameFor(from, to)))
                }
            }
        })
    }

    private fun decode(text: String): RoadGraph? {
        val json = JSONObject(text)
        val nodesJson = json.getJSONArray("nodes")
        val nodes = HashMap<Long, LatLng>(nodesJson.length())
        for (i in 0 until nodesJson.length()) {
            val node = nodesJson.getJSONArray(i)
            nodes[node.getLong(0)] = LatLng(node.getDouble(1), node.getDouble(2))
        }

        val adjacency = HashMap<Long, MutableList<Long>>()
        val streetNames = HashMap<Long, String>()
        val edgesJson = json.getJSONArray("edges")
        for (i in 0 until edgesJson.length()) {
            val edge = edgesJson.getJSONArray(i)
            val from = edge.getLong(0)
            val to = edge.getLong(1)
            if (nodes.containsKey(from) && nodes.containsKey(to)) {
                adjacency.getOrPut(from) { mutableListOf() }.add(to)
                val name = edge.optString(2)
                if (name.isNotBlank() && name != "the road") streetNames[edgeKey(from, to)] = name
            }
        }
        if (nodes.isEmpty() || adjacency.isEmpty()) return null
        return RoadGraph(
            nodes,
            adjacency,
            streetNames,
            LatLng(json.getDouble("latitude"), json.getDouble("longitude")),
            json.getInt("radiusMeters")
        )
    }
}
