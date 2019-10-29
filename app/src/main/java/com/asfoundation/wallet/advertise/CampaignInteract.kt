package com.asfoundation.wallet.advertise

import com.appcoins.wallet.bdsbilling.WalletService
import com.asf.wallet.BuildConfig
import com.asfoundation.wallet.interact.AutoUpdateInteract
import com.asfoundation.wallet.interact.CreateWalletInteract
import com.asfoundation.wallet.interact.FindDefaultWalletInteract
import com.asfoundation.wallet.poa.PoaInformationModel
import com.asfoundation.wallet.poa.ProofSubmissionData
import com.asfoundation.wallet.repository.WalletNotFoundException
import com.asfoundation.wallet.service.Campaign
import com.asfoundation.wallet.service.CampaignService
import com.asfoundation.wallet.service.CampaignStatus
import io.reactivex.Single
import java.net.UnknownHostException

class CampaignInteract(private val campaignService: CampaignService,
                       private val walletService: WalletService,
                       private val createWalletInteract: CreateWalletInteract,
                       private val autoUpdateInteract: AutoUpdateInteract,
                       private val errorMapper: AdvertisingThrowableCodeMapper,
                       private val defaultWalletInteract: FindDefaultWalletInteract) : Advertising {

  override fun getCampaign(packageName: String, versionCode: Int): Single<CampaignDetails> {
    if (isHardUpdateRequired()) {
      return Single.just(CampaignDetails(Advertising.CampaignAvailabilityType.UPDATE_REQUIRED))
    }
    return walletService.getWalletAddress()
        .onErrorResumeNext {
          createWalletInteract.create()
              .map { it.address }
        }
        .flatMap { campaignService.getCampaign(it, packageName, versionCode) }
        .map { map(it) }
        .onErrorReturn { CampaignDetails(errorMapper.map(it)) }
  }

  private fun map(campaign: Campaign) =
      if (campaign.campaignStatus == CampaignStatus.AVAILABLE) CampaignDetails(
          Advertising.CampaignAvailabilityType.AVAILABLE,
          campaign.campaignId) else CampaignDetails(
          Advertising.CampaignAvailabilityType.UNAVAILABLE,
          campaign.campaignId, campaign.hoursRemaining, campaign.minutesRemaining)

  override fun hasWalletPrepared(chainId: Int,
                                 packageName: String,
                                 versionCode: Int): Single<ProofSubmissionData> {
    if (isHardUpdateRequired()) {
      return Single.just(
          ProofSubmissionData(ProofSubmissionData.RequirementsStatus.UPDATE_REQUIRED))
    }
    if (!isCorrectNetwork(chainId)) {
      return if (isKnownNetwork(chainId)) {
        Single.just(
            ProofSubmissionData(ProofSubmissionData.RequirementsStatus.WRONG_NETWORK))
      } else {
        Single.just(
            ProofSubmissionData(ProofSubmissionData.RequirementsStatus.UNKNOWN_NETWORK))
      }
    }
    return defaultWalletInteract.find()
        .flatMap {
          campaignService.getCampaign(it.address,
              packageName, versionCode)
        }
        .map {
          if (isEligible(it.campaignStatus)) {
            ProofSubmissionData(ProofSubmissionData.RequirementsStatus.READY)
          } else {
            ProofSubmissionData(ProofSubmissionData.RequirementsStatus.NOT_ELIGIBLE,
                it.hoursRemaining, it.minutesRemaining)
          }
        }
        .onErrorReturn {
          when (it) {
            is WalletNotFoundException -> ProofSubmissionData(
                ProofSubmissionData.RequirementsStatus.NO_WALLET)
            is UnknownHostException -> ProofSubmissionData(
                ProofSubmissionData.RequirementsStatus.NO_NETWORK)
            else -> throw it
          }
        }
  }

  private fun isEligible(campaignStatus: CampaignStatus): Boolean {
    return campaignStatus == CampaignStatus.AVAILABLE
  }

  private fun isKnownNetwork(chainId: Int): Boolean {
    return chainId == 1 || chainId == 3
  }

  private fun isCorrectNetwork(chainId: Int): Boolean {
    return chainId == 3 && BuildConfig.DEBUG || chainId == 1 && !BuildConfig.DEBUG
  }

  private fun isHardUpdateRequired(): Boolean {
    val autoUpdateModel = autoUpdateInteract.getAutoUpdateModel()
        .blockingGet()
    return autoUpdateInteract.isHardUpdateRequired(autoUpdateModel.blackList,
        autoUpdateModel.updateVersionCode, autoUpdateModel.updateMinSdk)
  }

  override fun retrievePoaInformation(address: String): Single<PoaInformationModel> {
    return campaignService.retrievePoaInformation(address)
  }

}
