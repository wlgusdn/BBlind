package com.test.moon.bblind

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.widget.Toast


class Store : Fragment(),BillingProcessor.IBillingHandler{
    override fun onPurchaseHistoryRestored() {
        Toast.makeText(context,"Restored",Toast.LENGTH_LONG).show()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    lateinit var bp : BillingProcessor
    private lateinit var text: ListView
    private lateinit var btn : Button
    override fun onBillingInitialized() {
        Toast.makeText(context,"Initialized",Toast.LENGTH_LONG).show()

    }



    override fun onProductPurchased(productId: String, details: TransactionDetails?) {

        val builder = AlertDialog.Builder(context!!)
        builder.setMessage("구매 성공 하였습니다.")
                .setCancelable(false)
                .setPositiveButton("확인", DialogInterface.OnClickListener { dialog, id ->
                    // to do action
                })
        val alert = builder.create()
        alert.show()
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        Toast.makeText(context,"Error",Toast.LENGTH_LONG).show()

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bp = BillingProcessor(activity, resources.getString(R.string.license), this)
        bp.initialize()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.store, container, false) as View
        text = view.findViewById(R.id.list)
        btn = view.findViewById(R.id.testbtn)
        btn.setOnClickListener { bp.purchase(activity,"heart50") }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(!bp.handleActivityResult(requestCode,resultCode,data))
        {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun onDestroy() {
        if(bp != null)
        {
            bp.release()
        }
        super.onDestroy()
    }
}