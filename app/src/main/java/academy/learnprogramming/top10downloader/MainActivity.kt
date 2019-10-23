package academy.learnprogramming.top10downloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import kotlin.properties.Delegates

private const val STATE_FEED_URL = "FeedUrl"
private const val STATE_FEED_LIMIT = "FeedLimit"

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageUrl: String = ""

    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageUrl = $imageUrl
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"
    private var downloadData: DownloadData? = null

    private var feedUrl =
        "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            feedUrl = savedInstanceState?.getString(STATE_FEED_URL, feedUrl)
            feedLimit = savedInstanceState?.getInt(STATE_FEED_LIMIT)
        }

        setContentView(R.layout.activity_main)

        downloadUrl(feedUrl.format(feedLimit))
        Log.d(tag, "onCreate done")
    }

    private fun downloadUrl(feedURL: String) {
        Log.d(tag, "downloadUrl start AsyncTask")
        downloadData = DownloadData(this, xmlListView)
        downloadData?.execute(feedURL)
        Log.d(tag, "downloadUrl done")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)

        if (feedLimit == 10) {
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val oldFeedUrl = feedUrl
        var feedUrlChanged = false
        var feedLimitChanged = false
        when (item.itemId) {
            R.id.mnuFree ->
                feedUrl =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid ->
                feedUrl =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs ->
                feedUrl =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnu10, R.id.mnu25 -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                    feedLimitChanged = true
                    Log.d(
                        tag,
                        "onOptionsItemSelected: ${item.title} setting feedLimit to $feedLimit"
                    )
                } else {
                    Log.d(tag, "onOptionsItemSelected: ${item.title} setting feedLimit unchanged")
                }
            }
            R.id.mnuRefresh -> {
                downloadUrl(feedUrl.format(feedLimit))
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

        if (oldFeedUrl != feedUrl) {
            feedUrlChanged = true
        }

        if (feedUrlChanged || feedLimitChanged) {
            downloadUrl(feedUrl.format(feedLimit))
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_FEED_LIMIT, feedLimit)
        outState.putString(STATE_FEED_URL, feedUrl)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) :
            AsyncTask<String, Void, String>() {
            private val tag = "DownloadData"

            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                val feedAdapter =
                    FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(tag, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(tag, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }
        }
    }


}
