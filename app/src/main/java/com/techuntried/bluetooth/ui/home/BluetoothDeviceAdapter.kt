package com.techuntried.bluetooth.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.techuntried.bluetooth.databinding.BluetoothDeviceItemBinding
import com.techuntried.bluetooth.domain.model.BluetoothDeviceItem


class BluetoothDeviceAdapter :
    ListAdapter<BluetoothDeviceItem, BluetoothDeviceAdapter.MyViewHolder>(BluetoothDeviceDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MyViewHolder private constructor(private val binding: BluetoothDeviceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BluetoothDeviceItem
        ) {
            binding.item = item

        }

        companion object {
            fun from(parent: ViewGroup): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = BluetoothDeviceItemBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(view)

            }
        }
    }

}


class BluetoothDeviceDiffUtil : DiffUtil.ItemCallback<BluetoothDeviceItem>() {
    override fun areItemsTheSame(
        oldItem: BluetoothDeviceItem,
        newItem: BluetoothDeviceItem
    ): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(
        oldItem: BluetoothDeviceItem,
        newItem: BluetoothDeviceItem
    ): Boolean {
        return oldItem == newItem
    }

}

