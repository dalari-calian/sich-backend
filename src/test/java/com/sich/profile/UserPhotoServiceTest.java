package com.sich.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.BusinessRuleException;
import com.sich.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class UserPhotoServiceTest {

    private static final String PNG = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[] { 1, 2 });

    @Mock
    private UserPhotoRepository userPhotoRepository;

    @InjectMocks
    private UserPhotoService userPhotoService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
    }

    private UserPhotoEntity photo(String image) {
        UserPhotoEntity photo = new UserPhotoEntity();
        photo.setUser(user);
        photo.setImage(image);
        return photo;
    }

    @Test
    void findByUser_shouldReturnImage_whenUserHasPhoto() {
        when(userPhotoRepository.findByUserId(1L)).thenReturn(Optional.of(photo(PNG)));

        assertThat(userPhotoService.findByUser(user)).isEqualTo(PNG);
    }

    @Test
    void findByUser_shouldReturnNull_whenUserHasNoPhoto() {
        when(userPhotoRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThat(userPhotoService.findByUser(user)).isNull();
    }

    @Test
    void save_shouldCreatePhoto_whenUserHasNone() {
        when(userPhotoRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPhotoRepository.save(any(UserPhotoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = userPhotoService.save(user, PNG);

        assertThat(result).isEqualTo(PNG);
        verify(userPhotoRepository).save(any(UserPhotoEntity.class));
    }

    @Test
    void save_shouldReplaceExistingPhoto() {
        UserPhotoEntity existing = photo("data:image/png;base64,AA==");
        when(userPhotoRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(userPhotoRepository.save(existing)).thenReturn(existing);

        userPhotoService.save(user, PNG);

        assertThat(existing.getImage()).isEqualTo(PNG);
    }

    @Test
    void save_shouldRejectInvalidImageWithoutSaving() {
        assertThatThrownBy(() -> userPhotoService.save(user, "data:image/gif;base64,AA=="))
                .isInstanceOf(BusinessRuleException.class);
        verify(userPhotoRepository, never()).save(any());
    }

    @Test
    void save_shouldRejectImageLargerThanOneMegabyte() {
        String big = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(new byte[UserPhotoService.MAX_PHOTO_BYTES + 1]);

        assertThatThrownBy(() -> userPhotoService.save(user, big))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem deve ter no máximo 1 MB");
    }

    @Test
    void delete_shouldRemovePhoto_whenPresent() {
        UserPhotoEntity existing = photo(PNG);
        when(userPhotoRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        userPhotoService.delete(user);

        verify(userPhotoRepository).delete(existing);
    }

    @Test
    void delete_shouldDoNothing_whenUserHasNoPhoto() {
        when(userPhotoRepository.findByUserId(1L)).thenReturn(Optional.empty());

        userPhotoService.delete(user);

        verify(userPhotoRepository, never()).delete(any());
    }

    @Test
    void findByProviderIds_shouldMapPhotosById() {
        when(userPhotoRepository.findProviderPhotos(Set.of(10L, 11L))).thenReturn(List.of(new OwnerPhoto(10L, PNG)));

        assertThat(userPhotoService.findByProviderIds(Set.of(10L, 11L))).containsExactlyEntriesOf(java.util.Map.of(10L, PNG));
    }

    @Test
    void findByCustomerIds_shouldMapPhotosById() {
        when(userPhotoRepository.findCustomerPhotos(Set.of(40L))).thenReturn(List.of(new OwnerPhoto(40L, PNG)));

        assertThat(userPhotoService.findByCustomerIds(Set.of(40L))).containsEntry(40L, PNG);
    }

    @Test
    void findByIds_shouldNotQuery_whenIdsAreEmpty() {
        assertThat(userPhotoService.findByProviderIds(Set.of())).isEmpty();
        assertThat(userPhotoService.findByCustomerIds(Set.of())).isEmpty();
        verify(userPhotoRepository, never()).findProviderPhotos(any());
        verify(userPhotoRepository, never()).findCustomerPhotos(any());
    }
}
