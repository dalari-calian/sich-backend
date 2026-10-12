package com.sich.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.product.dto.ProductRequest;
import com.sich.product.dto.ProductResponse;
import com.sich.product.dto.ProductTreeResponse;
import com.sich.product.dto.ProductUpdateRequest;
import com.sich.providerproduct.ProviderProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo de produtos em árvore.
 *
 * <p>Regra da SKU: cada nível é um segmento separado por ponto. A raiz recebe um
 * número sequencial ({@code 1}, {@code 2}...) e cada filho recebe a SKU do pai +
 * {@code .} + sequência de dois dígitos entre os irmãos ({@code 1.01}, {@code 1.01.02}).
 * A sequência é o maior segmento já usado entre os irmãos + 1. A SKU é gerada pelo
 * sistema e não muda ao editar nome/descrição.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String SKU_SEPARATOR = ".";

    /** Ordena pela SKU comparando cada nível como número (2 antes de 10, 1.99 antes de 1.100). */
    private static final Comparator<ProductEntity> BY_SKU = (a, b) -> compareSku(a.getSku(), b.getSku());

    private final ProductRepository productRepository;
    private final ProviderProductRepository providerProductRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        ProductEntity product = new ProductEntity();
        product.setName(request.name());
        product.setDescription(request.description());

        if (request.parentId() == null) {
            product.setSku(String.valueOf(nextSequence(productRepository.findByParentIsNull())));
        } else {
            ProductEntity parent = productRepository.findByIdForUpdate(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto pai não encontrado"));
            int sequence = nextSequence(productRepository.findByParentId(parent.getId()));
            product.setParent(parent);
            product.setSku(parent.getSku() + SKU_SEPARATOR + String.format("%02d", sequence));
        }

        return ProductResponse.of(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        ProductEntity product = findEntityById(id);
        product.setName(request.name());
        product.setDescription(request.description());
        return ProductResponse.of(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        ProductEntity product = findEntityById(id);
        if (productRepository.existsByParentId(id)) {
            throw new BusinessRuleException("Não é possível excluir um produto que possui filhos");
        }
        if (providerProductRepository.existsByProductId(id)) {
            throw new BusinessRuleException("Não é possível excluir um produto prestado por algum prestador");
        }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.of(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findRoots() {
        return productRepository.findByParentIsNull().stream()
                .sorted(BY_SKU)
                .map(ProductResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findChildren(Long id) {
        findEntityById(id);
        return productRepository.findByParentId(id).stream()
                .sorted(BY_SKU)
                .map(ProductResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findParent(Long id) {
        ProductEntity parent = findEntityById(id).getParent();
        if (parent == null) {
            throw new ResourceNotFoundException("Produto raiz não possui pai");
        }
        return ProductResponse.of(parent);
    }

    /**
     * Caminho da raiz até o pai direto do produto, montado a partir dos prefixos da SKU
     * ({@code 1.01.02} → {@code 1}, {@code 1.01}) em uma única consulta.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> findAncestors(Long id) {
        String sku = findEntityById(id).getSku();
        List<String> ancestorSkus = new ArrayList<>();
        int index = sku.indexOf(SKU_SEPARATOR);
        while (index > 0) {
            ancestorSkus.add(sku.substring(0, index));
            index = sku.indexOf(SKU_SEPARATOR, index + 1);
        }
        if (ancestorSkus.isEmpty()) {
            return List.of();
        }
        return productRepository.findBySkuIn(ancestorSkus).stream()
                .sorted(BY_SKU)
                .map(ProductResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductTreeResponse> findTree() {
        return buildTree(productRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ProductTreeResponse findSubtree(Long id) {
        ProductEntity product = findEntityById(id);
        List<ProductEntity> nodes = new ArrayList<>();
        nodes.add(product);
        nodes.addAll(productRepository.findBySkuStartingWith(product.getSku() + SKU_SEPARATOR));
        return buildTree(nodes).getFirst();
    }

    @Transactional(readOnly = true)
    public ProductEntity findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    }

    private int nextSequence(List<ProductEntity> siblings) {
        return siblings.stream()
                .map(ProductEntity::getSku)
                .mapToInt(sku -> Integer.parseInt(sku.substring(sku.lastIndexOf(SKU_SEPARATOR) + 1)))
                .max()
                .orElse(0) + 1;
    }

    /**
     * Monta a árvore em memória. Após ordenar pela SKU o pai sempre vem antes dos
     * filhos; o pai de cada nó é a SKU sem o último segmento.
     */
    private List<ProductTreeResponse> buildTree(List<ProductEntity> products) {
        Map<String, ProductTreeResponse> nodesBySku = new LinkedHashMap<>();
        List<ProductTreeResponse> roots = new ArrayList<>();
        for (ProductEntity product : products.stream().sorted(BY_SKU).toList()) {
            ProductTreeResponse node = ProductTreeResponse.of(product);
            nodesBySku.put(product.getSku(), node);
            int lastSeparator = product.getSku().lastIndexOf(SKU_SEPARATOR);
            ProductTreeResponse parent = lastSeparator < 0
                    ? null
                    : nodesBySku.get(product.getSku().substring(0, lastSeparator));
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    private static int compareSku(String a, String b) {
        String[] left = a.split(Pattern.quote(SKU_SEPARATOR));
        String[] right = b.split(Pattern.quote(SKU_SEPARATOR));
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            int result = Integer.compare(Integer.parseInt(left[i]), Integer.parseInt(right[i]));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(left.length, right.length);
    }
}
