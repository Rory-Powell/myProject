package rpowell.blockchain.services;

import rpowell.blockchain.domain.PublicKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpowell.blockchain.repositories.PublicKeyRepository;

@Service
@Transactional
public class PublicKeyService {

    @Autowired
    PublicKeyRepository publicKeyRepository;

    public PublicKey findByKey(String key) {
        return publicKeyRepository.findByPublicKey(key);
    }

    public void saveKey(PublicKey key) {
        publicKeyRepository.save(key);
    }

    public void saveAllKeys(Iterable<PublicKey> publicKeys) {
        publicKeyRepository.save(publicKeys);
    }
}
