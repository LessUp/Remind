package app.lessup.remind.di

import android.content.Context
import androidx.room.Room
import app.lessup.remind.data.db.AppDatabase
import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "lessup_remind.db").build()

    @Provides
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideSubDao(db: AppDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    @Singleton
    fun provideItemRepo(dao: ItemDao) = ItemRepository(dao)

    @Provides
    @Singleton
    fun provideSubRepo(dao: SubscriptionDao) = SubscriptionRepository(dao)
}
