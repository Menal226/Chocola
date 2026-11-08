package com.sosauce.chocola.wear.app.di

import com.sosauce.chocola.wear.core.data.PhoneRepository
import com.sosauce.chocola.wear.core.presentation.player.MusicViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearModule = module {
    singleOf(::PhoneRepository)

    viewModelOf(::MusicViewModel)
}
