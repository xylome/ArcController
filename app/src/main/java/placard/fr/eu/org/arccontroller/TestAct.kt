package placard.fr.eu.org.arccontroller

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity

/**
 * Created by xylome on 10/11/2017.
 */
class TestAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.test)
    }
}