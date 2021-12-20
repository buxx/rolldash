package fr.bux.rollingdashboard

import kotlinx.coroutines.flow.Flow

class SystemDataRepository(private val SystemDataDao: SystemDataDao) {
    val systemData: Flow<SystemData> = SystemDataDao.flow()
}
