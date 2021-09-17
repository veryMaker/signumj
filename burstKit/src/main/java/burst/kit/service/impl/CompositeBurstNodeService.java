package burst.kit.service.impl;

import burst.kit.entity.*;
import burst.kit.entity.response.*;
import burst.kit.service.BurstNodeService;
import burst.kit.util.BurstKitUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompositeBurstNodeService implements BurstNodeService {
    private final BurstNodeService[] burstNodeServices;

    /**
     * @param burstNodeServices The burst node services this will wrap, in order of priority
     */
    public CompositeBurstNodeService(BurstNodeService... burstNodeServices) {
        if (burstNodeServices == null || burstNodeServices.length == 0) throw new IllegalArgumentException("No Burst Node Services Provided");
        this.burstNodeServices = burstNodeServices;
    }

    private <T> Single<T> compositeSingle(Collection<Single<T>> singles) {
        return Single.create((SingleEmitter<T> emitter) -> {
            AtomicInteger errorCount = new AtomicInteger(0);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            emitter.setCancellable(compositeDisposable::dispose);
            for (Single<T> single : singles) {
                compositeDisposable.add(single.subscribe(emitter::onSuccess, error -> {
                    synchronized (errorCount) {
                        if (errorCount.incrementAndGet() == singles.size()) { // Every single has errored
                            emitter.tryOnError(error);
                        }
                    }
                }));
            }
        })
                .subscribeOn(BurstKitUtils.defaultBurstNodeServiceScheduler());
    }

    private synchronized <T> void doIfUsedObservable(ObservableEmitter<T> emitter, AtomicInteger usedObservable, AtomicReferenceArray<Disposable> disposables, int myI, Runnable runnable) {
        int used = usedObservable.get();
        if (used == -1) {
            // We are the first!
            usedObservable.set(myI);
            runnable.run();
            // Kill all of the others.
            Disposable myDisposable = disposables.get(myI);
            disposables.set(myI, null);
            emitter.setCancellable(() -> {
                if (myDisposable != null) {
                    myDisposable.dispose();
                }
            }); // Calling this calls the previous one, so all of the others get disposed.
        } else if (used == myI) {
            // We are the used observable.
            runnable.run();
        }
    }

    private <T> Observable<T> compositeObservable(List<Observable<T>> observables) {
        return Observable.create((ObservableEmitter<T> emitter) -> {
            AtomicInteger usedObservable = new AtomicInteger(-1);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicReferenceArray<Disposable> disposables = new AtomicReferenceArray<>(observables.size());
            emitter.setCancellable(() -> {
                for (int i = 0; i < disposables.length(); i++) {
                    Disposable disposable = disposables.get(i);
                    if (disposable != null) disposable.dispose();
                }
            });
            for (int i = 0; i < observables.size(); i++) {
                final int myI = i;
                Observable<T> observable = observables.get(i);
                disposables.set(myI, observable.subscribe(t -> doIfUsedObservable(emitter, usedObservable, disposables, myI, () -> emitter.onNext(t)),
                        error -> {
                            synchronized (errorCount) {
                                if (errorCount.incrementAndGet() == observables.size() || usedObservable.get() == myI) { // Every single has errored
                                    emitter.tryOnError(error);
                                }
                            }
                        },
                        () -> doIfUsedObservable(emitter, usedObservable, disposables, myI, emitter::onComplete)));
            }
        })
                .subscribeOn(BurstKitUtils.defaultBurstNodeServiceScheduler());
    }

    private <T, U> List<U> map(T[] ts, Function<T, U> mapper) {
        return Arrays.stream(ts)
                .map(mapper)
                .collect(Collectors.toList());
    }

    private <T> Single<T> performFastest(Function<BurstNodeService, Single<T>> function) {
        return compositeSingle(map(burstNodeServices, function));
    }

    private <T> Observable<T> performFastestObservable(Function<BurstNodeService, Observable<T>> function) {
        return compositeObservable(map(burstNodeServices, function));
    }

    private <T> Single<T> performOnOne(Function<BurstNodeService, Single<T>> function) {
        List<Single<T>> singles = map(burstNodeServices, function);
        for (int i = singles.size() - 2; i >= 0; i--) {
            singles.set(i, singles.get(i).onErrorResumeNext(singles.get(i+1)));
        }
        return singles.get(0);
    }

    @Override
    public Single<Block> getBlock(BurstID block) {
        return performFastest(service -> service.getBlock(block));
    }

    @Override
    public Single<Block> getBlock(int height) {
        return performFastest(service -> service.getBlock(height));
    }

    @Override
    public Single<Block> getBlock(BurstTimestamp timestamp) {
        return performFastest(service -> service.getBlock(timestamp));
    }

    @Override
    public Single<BurstID> getBlockId(int height) {
        return performFastest(service -> service.getBlockId(height));
    }

    @Override
    public Single<Block[]> getBlocks(int firstIndex, int lastIndex) {
        return performFastest(service -> service.getBlocks(firstIndex, lastIndex));
    }

    @Override
    public Single<Constants> getConstants() {
        return performFastest(BurstNodeService::getConstants);
    }

    @Override
    public Single<Account> getAccount(BurstAddress accountId) {
        return performFastest(service -> service.getAccount(accountId));
    }

    @Override
    public Single<AT[]> getAccountATs(BurstAddress accountId) {
        return performFastest(service -> service.getAccountATs(accountId));
    }

    @Override
    public Single<BurstID[]> getAccountBlockIDs(BurstAddress accountId) {
        return performFastest(service -> service.getAccountBlockIDs(accountId));
    }

    @Override
    public Single<Block[]> getAccountBlocks(BurstAddress accountId) {
        return performFastest(service -> service.getAccountBlocks(accountId));
    }

    @Override
    public Single<BurstID[]> getAccountTransactionIDs(BurstAddress accountId) {
        return performFastest(service -> service.getAccountTransactionIDs(accountId));
    }

    @Override
    public Single<Transaction[]> getAccountTransactions(BurstAddress accountId, Integer firstIndex, Integer lastIndex, Boolean includeIndirect) {
        return performFastest(service -> service.getAccountTransactions(accountId, firstIndex, lastIndex, includeIndirect));
    }

    @Override
    public Single<Transaction[]> getUnconfirmedTransactions(BurstAddress accountId) {
        return performFastest(service -> service.getUnconfirmedTransactions(accountId));
    }

    @Override
    public Single<BurstAddress[]> getAccountsWithRewardRecipient(BurstAddress accountId) {
        return performFastest(service -> service.getAccountsWithRewardRecipient(accountId));
    }

    @Override
    public Single<AssetBalance[]> getAssetBalances(BurstID assetId, Integer firstIndex, Integer lastIndex) {
        return performFastest(service -> service.getAssetBalances(assetId, firstIndex, lastIndex));
    }

    @Override
    public Single<Asset> getAsset(BurstID assetId) {
        return performFastest(service -> service.getAsset(assetId));
    }

    @Override
    public Single<AssetTrade[]> getAssetTrades(BurstID assetId, BurstAddress account, Integer firstIndex, Integer lastIndex) {
        return performFastest(service -> service.getAssetTrades(assetId, account, firstIndex, lastIndex));
    }

    @Override
    public Single<AssetOrder[]> getAskOrders(BurstID assetId) {
        return performFastest(service -> service.getAskOrders(assetId));
    }

    @Override
    public Single<AssetOrder[]> getBidOrders(BurstID assetId) {
        return performFastest(service -> service.getBidOrders(assetId));
    }

    @Override
    public Single<AT> getAt(BurstAddress at) {
        return performFastest(service -> service.getAt(at));
    }

    @Override
    public Single<BurstAddress[]> getAtIds() {
        return performFastest(service -> service.getAtIds());
    }

    @Override
    public Single<Transaction> getTransaction(BurstID transactionId) {
        return performFastest(service -> service.getTransaction(transactionId));
    }

    @Override
    public Single<Transaction> getTransaction(byte[] fullHash) {
        return performFastest(service -> service.getTransaction(fullHash));
    }

    @Override
    public Single<byte[]> getTransactionBytes(BurstID transactionId) {
        return performFastest(service -> service.getTransactionBytes(transactionId));
    }

    @Override
    public Single<byte[]> generateTransaction(BurstAddress recipient, byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateTransaction(recipient, senderPublicKey, amount, fee, deadline));
    }

    @Override
    public Single<byte[]> generateTransactionWithMessage(BurstAddress recipient, byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline, String message) {
        return performFastest(service -> service.generateTransactionWithMessage(recipient, senderPublicKey, amount, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithMessage(BurstAddress recipient, byte[] senderPublicKey, BurstValue fee, int deadline, String message) {
        return performFastest(service -> service.generateTransactionWithMessage(recipient, senderPublicKey, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithMessage(BurstAddress recipientAddress, byte[] recipientPublicKey, byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline, String message) {
        return performFastest(service -> service.generateTransactionWithMessage(recipientAddress, recipientPublicKey, senderPublicKey, amount, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithMessage(BurstAddress recipientAddress, byte[] recipientPublicKey, byte[] senderPublicKey, BurstValue fee, int deadline, String message) {
        return performFastest(service -> service.generateTransactionWithMessage(recipientAddress, recipientPublicKey, senderPublicKey, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithMessage(BurstAddress recipient, byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline, byte[] message) {
        return performFastest(service -> service.generateTransactionWithMessage(recipient, senderPublicKey, amount, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithMessage(BurstAddress recipient, byte[] senderPublicKey, BurstValue fee, int deadline, byte[] message) {
        return performFastest(service -> service.generateTransactionWithMessage(recipient, senderPublicKey, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithEncryptedMessage(BurstAddress recipient, byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline, BurstEncryptedMessage message) {
        return performFastest(service -> service.generateTransactionWithEncryptedMessage(recipient, senderPublicKey, amount, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithEncryptedMessage(BurstAddress recipient, byte[] senderPublicKey, BurstValue fee, int deadline, BurstEncryptedMessage message) {
        return performFastest(service -> service.generateTransactionWithEncryptedMessage(recipient, senderPublicKey, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithEncryptedMessageToSelf(BurstAddress recipient, byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline, BurstEncryptedMessage message) {
        return performFastest(service -> service.generateTransactionWithEncryptedMessageToSelf(recipient, senderPublicKey, amount, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransactionWithEncryptedMessageToSelf(BurstAddress recipient, byte[] senderPublicKey, BurstValue fee, int deadline, BurstEncryptedMessage message) {
        return performFastest(service -> service.generateTransactionWithEncryptedMessageToSelf(recipient, senderPublicKey, fee, deadline, message));
    }

    @Override
    public Single<FeeSuggestion> suggestFee() {
        return performFastest(BurstNodeService::suggestFee);
    }

    @Override
    public Observable<MiningInfo> getMiningInfo() {
        return performFastestObservable(BurstNodeService::getMiningInfo);
    }

    @Override
    public Single<TransactionBroadcast> broadcastTransaction(byte[] transactionBytes) {
        return performFastest(service -> service.broadcastTransaction(transactionBytes));
    }

    @Override
    public Single<BurstAddress> getRewardRecipient(BurstAddress account) {
        return performFastest(service -> service.getRewardRecipient(account));
    }

    @Override
    public Single<Long> submitNonce(String passphrase, String nonce, BurstID accountId) {
        return performOnOne(service -> service.submitNonce(passphrase, nonce, accountId));
    }

    @Override
    public Single<byte[]> generateMultiOutTransaction(byte[] senderPublicKey, BurstValue fee, int deadline, Map<BurstAddress, BurstValue> recipients) throws IllegalArgumentException {
        return performFastest(service -> service.generateMultiOutTransaction(senderPublicKey, fee, deadline, recipients));
    }

    @Override
    public Single<byte[]> generateMultiOutSameTransaction(byte[] senderPublicKey, BurstValue amount, BurstValue fee, int deadline, Set<BurstAddress> recipients) throws IllegalArgumentException {
        return performFastest(service -> service.generateMultiOutSameTransaction(senderPublicKey, amount, fee, deadline, recipients));
    }

    @Override
    public Single<byte[]> generateCreateATTransaction(byte[] senderPublicKey, BurstValue fee, int deadline, String name, String description, byte[] creationBytes) {
        return performFastest(service -> service.generateCreateATTransaction(senderPublicKey, fee, deadline, name, description, creationBytes));
    }

    @Override
    public Single<byte[]> generateTransferAssetTransaction(byte[] senderPublicKey, BurstAddress recipient, BurstID assetId, BurstValue quantity, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateTransferAssetTransaction(senderPublicKey, recipient, assetId, quantity, fee, deadline));
    }

    @Override
    public Single<byte[]> generateIssueAssetTransaction(byte[] senderPublicKey, String name, String description, BurstValue quantity, int decimals, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateIssueAssetTransaction(senderPublicKey, name, description, quantity, decimals, fee, deadline));
    }

    @Override
    public Single<byte[]> generateTransferAssetTransactionWithMessage(byte[] senderPublicKey, BurstAddress recipient, BurstID assetId, BurstValue quantity, BurstValue fee, int deadline, String message) {
        return performFastest(service -> service.generateTransferAssetTransactionWithMessage(senderPublicKey, recipient, assetId, quantity, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generateTransferAssetTransactionWithEncryptedMessage(byte[] senderPublicKey, BurstAddress recipient, BurstID assetId, BurstValue quantity, BurstValue fee, int deadline, BurstEncryptedMessage message) {
        return performFastest(service -> service.generateTransferAssetTransactionWithEncryptedMessage(senderPublicKey, recipient, assetId, quantity, fee, deadline, message));
    }

    @Override
    public Single<byte[]> generatePlaceAskOrderTransaction(byte[] senderPublicKey, BurstID assetId, BurstValue quantity, BurstValue price, BurstValue fee, int deadline) {
        return performFastest(service -> service.generatePlaceAskOrderTransaction(senderPublicKey, assetId, quantity, price, fee, deadline));
    }

    @Override
    public Single<byte[]> generatePlaceBidOrderTransaction(byte[] senderPublicKey, BurstID assetId, BurstValue quantity, BurstValue price, BurstValue fee, int deadline) {
        return performFastest(service -> service.generatePlaceBidOrderTransaction(senderPublicKey, assetId, quantity, price, fee, deadline));
    }

    @Override
    public Single<byte[]> generateCancelAskOrderTransaction(byte[] senderPublicKey, BurstID orderId, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateCancelAskOrderTransaction(senderPublicKey, orderId, fee, deadline));
    }
    
    @Override
    public Single<byte[]> generateCancelBidOrderTransaction(byte[] senderPublicKey, BurstID orderId, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateCancelBidOrderTransaction(senderPublicKey, orderId, fee, deadline));
    }

    @Override
    public Single<byte[]> generateSubscriptionCreationTransaction(byte[] senderPublicKey, BurstValue amount, int frequency, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateSubscriptionCreationTransaction(senderPublicKey, amount, frequency, fee, deadline));
    }

    @Override
    public Single<byte[]> generateSubscriptionCancelTransaction(byte[] senderPublicKey, BurstID subscription, BurstValue fee, int deadline) {
        return performFastest(service -> service.generateSubscriptionCancelTransaction(senderPublicKey, subscription, fee, deadline));
    }

    @Override
    public void close() throws Exception {
        for (BurstNodeService burstNodeService : burstNodeServices) {
            burstNodeService.close();
        }
    }
}
