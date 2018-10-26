package placard.fr.eu.org.arccontroller

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by xylome on 07/11/2017.
 */
class MainActivity : Activity() {

    private val redCommand   = 82
    private val greenCommand = 71
    private val blueCommand  = 66

    private var myBTAdapter: BluetoothAdapter? = null
    private var myArcReactor: BluetoothDevice? = null
    private var myBTSocket : BluetoothSocket?  = null

    private var myColorFlow: Disposable? = null
    private var myBTFlow: Disposable? = null

    private lateinit var mBroadCastReceiver: BroadcastReceiver

    private var connected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()
        setupReceiver()
        connect_button.setOnClickListener({ connectBT() })
    }

    override fun onPause() {
        super.onPause()
        disconnectBT()
        unregisterReceiver(mBroadCastReceiver)
    }

    private fun setupReceiver() {
        mBroadCastReceiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
               Log.e("receiver", "Received:" + intent?.action)
            }
        }
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadCastReceiver, intentFilter)
    }

    private fun getBTConnectionObservable() : Observable<Boolean> {
        return Observable.create<Boolean> {
            e->
            myBTAdapter = BluetoothAdapter.getDefaultAdapter()
            myArcReactor = myBTAdapter?.getRemoteDevice(getString(R.string.arc_address))
            try {
                myBTSocket = myArcReactor?.createInsecureRfcommSocketToServiceRecord(
                        UUID.fromString(getString(R.string.RFCOMM_UUID)))
                myBTAdapter?.cancelDiscovery()
                myBTSocket?.connect()
                e.onNext(true)
            } catch (exception: Exception) {
                e.onNext(false)
                Log.e("MAIN", "Erreur de connexion BT:" + exception.message)
            }
        }
    }

    private fun createRedSliderObservable() : Observable<Pair<Int, Int>> {
        return Observable.create { e ->
            red_seek_bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
                        = e.onNext(Pair(redCommand, progress))
            })
            e.setCancellable { red_seek_bar.setOnSeekBarChangeListener(null) }
        }
    }

    private fun createGreenSliderObservable() : Observable<Pair<Int, Int>> {
        return Observable.create { e ->
            green_seek_bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
                        = e.onNext(Pair(greenCommand, progress))
            })
            e.setCancellable {
                Log.e("MainActivity", "Unresgistering green seekbar…")
                green_seek_bar.setOnSeekBarChangeListener(null) }
        }
    }

    private fun createBlueSliderObservable() : Observable<Pair<Int, Int>> {
        return Observable.create { e ->
            blue_seek_bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
                        = e.onNext(Pair(blueCommand, progress))
            })
            e.setCancellable { blue_seek_bar.setOnSeekBarChangeListener(null) }
        }
    }

    private fun connectBT() {
        if (!connected) {
            val btConnect = getBTConnectionObservable()

            myBTFlow = btConnect
                    .observeOn(Schedulers.io())
                    .subscribe {
                        connected = it
                        if (connected) setUPslideBars()
                    }
        }
    }

    private fun setUPslideBars() {
        val red = createRedSliderObservable().debounce(100, TimeUnit.MILLISECONDS)
        var green = createGreenSliderObservable().debounce(100, TimeUnit.MILLISECONDS)
        val blue = createBlueSliderObservable().debounce(100, TimeUnit.MILLISECONDS)

        val allFlows = Observable.merge(red, green, blue)

        myColorFlow = allFlows
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    sendMSG(it)
                }
    }

    private fun disconnectBT() {
        myBTSocket?.close()
        if (! myColorFlow!!.isDisposed) {
            myColorFlow?.dispose()
        }

        if (! myBTFlow!!.isDisposed) {
            myBTFlow?.dispose()
        }

        connect_button.setOnClickListener(null)

        connected = false
    }

    private fun sendMSG(msg: Pair<Int, Int>) {
        if (connected) {
            myBTSocket?.outputStream?.write(msg.first)
            myBTSocket?.outputStream?.write(msg.second)
        }
    }

    private fun show(msg: Pair<Int, Int>) {
        Toast.makeText(this, "Received: ${msg.first} - ${msg.second}", Toast.LENGTH_SHORT).show()
    }
}