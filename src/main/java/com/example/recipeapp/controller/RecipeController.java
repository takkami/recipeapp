package com.example.recipeapp.controller;

import com.example.recipeapp.model.Recipe;
import com.example.recipeapp.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

@Controller
public class RecipeController {

    private static final int MAX_CATEGORIES = 2; // 3から2に変更

    @Autowired
    private RecipeRepository recipeRepository;

    @GetMapping("/home")
    public String showHome(Model model) {
        model.addAttribute("recipes", recipeRepository.findAll());
        model.addAttribute("favoritesPage", false);
        return "home";
    }

    // レシピ作成画面を表示
    @GetMapping("/recipes/new")
    public String showRecipeForm(Model model) {
        model.addAttribute("recipe", new Recipe());
        return "recipe_form";
    }

    // カテゴリ数をバリデーションするヘルパーメソッド（改善版）
    private ValidationResult validateCategories(List<String> categories) {
        ValidationResult result = new ValidationResult();

        if (categories == null || categories.isEmpty()) {
            result.categories = new HashSet<>();
            result.isValid = true;
            return result;
        }

        // nullと空文字列を除外してから重複を除去
        Set<String> uniqueCategories = new HashSet<>();
        for (String cat : categories) {
            if (cat != null && !cat.trim().isEmpty()) {
                uniqueCategories.add(cat.trim());
            }
        }

        System.out.println("バリデーション - 受信カテゴリ（重複含む）: " + categories);
        System.out.println("バリデーション - ユニークカテゴリ数: " + uniqueCategories.size());
        System.out.println("バリデーション - カテゴリ内容: " + uniqueCategories);

        // カテゴリ数の制限チェック
        if (uniqueCategories.size() > MAX_CATEGORIES) {
            result.categories = uniqueCategories;
            result.isValid = false;
            result.errorMessage = "カテゴリは" + MAX_CATEGORIES + "つまでしか選択できません。現在" + uniqueCategories.size() + "つ選択されています。";
            System.out.println("カテゴリ制限エラー: " + uniqueCategories.size() + " > " + MAX_CATEGORIES);
            return result;
        }

        result.categories = uniqueCategories;
        result.isValid = true;
        return result;
    }

    // バリデーション結果を格納するクラス
    private static class ValidationResult {
        Set<String> categories;
        boolean isValid;
        String errorMessage;
    }

    // レシピを新規登録（改善版）
    @PostMapping("/recipes/new")
    public String submitRecipe(@RequestParam String title,
                               @RequestParam(required = false, defaultValue = "") String ingredients,
                               @RequestParam(required = false, defaultValue = "") String instructions,
                               @RequestParam(name = "favorite", defaultValue = "false") boolean favorite,
                               @RequestParam(required = false) String reference,
                               @RequestParam(value = "categories", required = false) List<String> categories,
                               @RequestParam(name = "image", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) throws IOException {

        System.out.println("新規登録 - 受信したカテゴリ（生データ）: " + categories);

        // 入力値の基本バリデーション
        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "タイトルは必須です。");
            return "redirect:/recipes/new";
        }

        // カテゴリの重複を除去してからバリデーション
        List<String> processedCategories = null;
        if (categories != null) {
            // 重複を除去したリストを作成
            processedCategories = new ArrayList<>(new HashSet<>(categories));
        }

        ValidationResult validationResult = validateCategories(processedCategories);
        if (!validationResult.isValid) {
            redirectAttributes.addFlashAttribute("errorMessage", validationResult.errorMessage);
            redirectAttributes.addFlashAttribute("recipe", createRecipeFromParams(title, ingredients, instructions, favorite, reference, validationResult.categories));
            return "redirect:/recipes/new";
        }

        Recipe recipe = new Recipe();
        recipe.setTitle(title.trim());
        recipe.setIngredients(ingredients != null ? ingredients : "");
        recipe.setInstructions(instructions != null ? instructions : "");
        recipe.setCategories(validationResult.categories);
        recipe.setFavorite(favorite);
        recipe.setReference(reference);

        System.out.println("新規登録 - 設定されたカテゴリ: " + validationResult.categories);

        // 画像アップロード処理
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);
            imageFile.transferTo(filePath.toFile());

            recipe.setImagePath("/uploads/" + fileName);
        }

        try {
            Recipe savedRecipe = recipeRepository.save(recipe);
            System.out.println("保存されたレシピID: " + savedRecipe.getId());
            System.out.println("保存されたカテゴリ: " + savedRecipe.getCategories());
            redirectAttributes.addFlashAttribute("successMessage", "レシピが正常に登録されました。");
        } catch (Exception e) {
            System.err.println("レシピ保存エラー: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "レシピの保存に失敗しました。");
            return "redirect:/recipes/new";
        }

        return "redirect:/home?loading=true";
    }

    // 編集画面を表示
    @GetMapping("/recipes/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid recipe ID: " + id));
        model.addAttribute("recipe", recipe);
        return "recipe_form";
    }

    // 編集内容を保存（改善版）
    @PostMapping("/recipes/update")
    public String updateRecipe(@RequestParam Long id,
                               @RequestParam String title,
                               @RequestParam(required = false, defaultValue = "") String ingredients,
                               @RequestParam(required = false, defaultValue = "") String instructions,
                               @RequestParam(name = "favorite", defaultValue = "false") boolean favorite,
                               @RequestParam(required = false) String reference,
                               @RequestParam(value = "categories", required = false) List<String> categories,
                               @RequestParam(name = "image", required = false) MultipartFile image,
                               @RequestParam(name = "deleteCurrentImage", defaultValue = "false") boolean deleteCurrentImage,
                               RedirectAttributes redirectAttributes
    ) {

        System.out.println("===== レシピ更新処理開始 =====");
        System.out.println("更新対象ID: " + id);
        System.out.println("タイトル: " + title);
        System.out.println("受信したカテゴリ（生データ）: " + categories);

        try {
            // 入力値の基本バリデーション
            if (title == null || title.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "タイトルは必須です。");
                return "redirect:/recipes/edit/" + id;
            }

            // カテゴリの重複を除去してからバリデーション
            List<String> processedCategories = null;
            if (categories != null) {
                // 重複を除去したリストを作成
                processedCategories = new ArrayList<>(new HashSet<>(categories));
            }

            ValidationResult validationResult = validateCategories(processedCategories);
            if (!validationResult.isValid) {
                redirectAttributes.addFlashAttribute("errorMessage", validationResult.errorMessage);
                return "redirect:/recipes/edit/" + id;
            }

            Recipe existingRecipe = recipeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid recipe ID: " + id));

            existingRecipe.setTitle(title.trim());
            existingRecipe.setIngredients(ingredients != null ? ingredients : "");
            existingRecipe.setInstructions(instructions != null ? instructions : "");
            existingRecipe.setFavorite(favorite);
            existingRecipe.setReference(reference);

            // カテゴリをクリアしてから新しいカテゴリを設定
            existingRecipe.clearCategories();
            existingRecipe.setCategories(validationResult.categories);

            System.out.println("更新 - 設定されたカテゴリ: " + validationResult.categories);

            // 画像処理
            if (image != null && !image.isEmpty()) {
                Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
                Files.createDirectories(uploadPath);

                // 古い画像があれば削除
                if (existingRecipe.getImagePath() != null) {
                    Path oldPath = uploadPath.resolve(Paths.get(existingRecipe.getImagePath()).getFileName());
                    Files.deleteIfExists(oldPath);
                }

                // 新しい画像を保存
                String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                image.transferTo(filePath.toFile());

                existingRecipe.setImagePath("/uploads/" + fileName);
            } else if (deleteCurrentImage) {
                // 画像削除フラグが立っている場合
                if (existingRecipe.getImagePath() != null) {
                    Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
                    Path oldPath = uploadPath.resolve(Paths.get(existingRecipe.getImagePath()).getFileName());
                    Files.deleteIfExists(oldPath);
                }
                existingRecipe.setImagePath(null);
            }

            Recipe savedRecipe = recipeRepository.save(existingRecipe);
            System.out.println("更新されたレシピID: " + savedRecipe.getId());
            System.out.println("更新されたカテゴリ: " + savedRecipe.getCategories());
            System.out.println("===== レシピ更新処理完了 =====");

            redirectAttributes.addFlashAttribute("successMessage", "レシピが正常に更新されました。");
            return "redirect:/home?loading=true";

        } catch (Exception e) {
            System.err.println("===== レシピ更新エラー =====");
            System.err.println("エラーメッセージ: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "レシピの更新に失敗しました: " + e.getMessage());
            return "redirect:/recipes/edit/" + id;
        }
    }

    // レシピ削除処理（フォームからのPOST - 既存のページ遷移用）
    @PostMapping("/recipes/{id}/delete")
    public String deleteRecipe(@PathVariable Long id,
                               @RequestParam(required = false) Boolean from,
                               @RequestParam(required = false) String category) {
        try {
            // 画像ファイルも削除
            Recipe recipe = recipeRepository.findById(id).orElse(null);
            if (recipe != null && recipe.getImagePath() != null) {
                Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
                Path imagePath = uploadPath.resolve(Paths.get(recipe.getImagePath()).getFileName());
                Files.deleteIfExists(imagePath);
            }

            recipeRepository.deleteById(id);
        } catch (Exception e) {
            System.err.println("レシピ削除エラー: " + e.getMessage());
        }

        if (Boolean.TRUE.equals(from)) {
            return "redirect:/recipes/favorites";
        }

        if (category != null && !category.isEmpty()) {
            try {
                String encodedCategory = URLEncoder.encode(category, "UTF-8");
                return "redirect:/recipes/category/" + encodedCategory;
            } catch (UnsupportedEncodingException e) {
                return "redirect:/home";
            }
        }

        return "redirect:/home";
    }

    // レシピ削除処理（AJAX用 - その場削除）
    @DeleteMapping("/recipes/{id}/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteRecipeAjax(@PathVariable Long id) {
        try {
            if (!recipeRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            // 画像ファイルも削除
            Recipe recipe = recipeRepository.findById(id).orElse(null);
            if (recipe != null && recipe.getImagePath() != null) {
                Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
                Path imagePath = uploadPath.resolve(Paths.get(recipe.getImagePath()).getFileName());
                Files.deleteIfExists(imagePath);
            }

            recipeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("AJAX削除エラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // お気に入りのみ表示
    @GetMapping("/recipes/favorites")
    public String showFavoriteRecipes(Model model) {
        model.addAttribute("recipes", recipeRepository.findByFavoriteTrue());
        model.addAttribute("favoritesPage", true);
        return "home";
    }

    // カテゴリ別表示
    @GetMapping("/recipes/category/{category}")
    public String showRecipesByCategory(@PathVariable String category, Model model) {
        List<Recipe> recipes = recipeRepository.findByCategory(category);
        model.addAttribute("recipes", recipes);
        model.addAttribute("categoryName", category);
        return "home";
    }

    // お気に入りトグル
    @PostMapping("/recipes/{id}/toggleFavorite")
    @ResponseBody
    public ResponseEntity<Boolean> toggleFavorite(@PathVariable Long id) {
        try {
            Recipe recipe = recipeRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("指定されたレシピが見つかりません ID: " + id));
            recipe.setFavorite(!recipe.isFavorite());
            recipeRepository.save(recipe);
            return ResponseEntity.ok(recipe.isFavorite());
        } catch (Exception e) {
            System.err.println("お気に入りトグルエラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 統計情報を取得するAPIエンドポイント
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRecipeStats() {
        try {
            List<Recipe> allRecipes = recipeRepository.findAll();
            List<Recipe> favoriteRecipes = recipeRepository.findByFavoriteTrue();

            // カテゴリ別の統計を計算
            Map<String, Long> categoryStats = allRecipes.stream()
                    .flatMap(recipe -> recipe.getCategories().stream())
                    .collect(Collectors.groupingBy(
                            category -> category,
                            Collectors.counting()
                    ));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRecipes", allRecipes.size());
            stats.put("favoriteRecipes", favoriteRecipes.size());
            stats.put("categoryStats", categoryStats);
            stats.put("averageRecipesPerCategory",
                    categoryStats.isEmpty() ? 0 :
                            categoryStats.values().stream().mapToLong(Long::longValue).average().orElse(0));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("統計情報取得エラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * データエクスポート機能
     */
    @GetMapping("/api/export")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> exportRecipeData() {
        try {
            List<Recipe> allRecipes = recipeRepository.findAll();

            List<Map<String, Object>> exportData = allRecipes.stream().map(recipe -> {
                Map<String, Object> recipeData = new HashMap<>();
                recipeData.put("id", recipe.getId());
                recipeData.put("title", recipe.getTitle());
                recipeData.put("ingredients", recipe.getIngredients());
                recipeData.put("instructions", recipe.getInstructions());
                recipeData.put("categories", recipe.getCategories());
                recipeData.put("favorite", recipe.isFavorite());
                recipeData.put("reference", recipe.getReference());
                recipeData.put("hasImage", recipe.getImagePath() != null);
                return recipeData;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            System.err.println("データエクスポートエラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 全レシピのカテゴリ一覧を取得
     */
    @GetMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<Recipe> allRecipes = recipeRepository.findAll();

            // カテゴリとその使用回数を取得
            Map<String, Long> categoryCount = allRecipes.stream()
                    .flatMap(recipe -> recipe.getCategories().stream())
                    .collect(Collectors.groupingBy(
                            category -> category,
                            Collectors.counting()
                    ));

            // カテゴリを使用回数の降順でソート
            List<Map.Entry<String, Long>> sortedCategories = categoryCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("categories", sortedCategories);
            result.put("totalCategories", categoryCount.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("カテゴリ取得エラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 検索機能の強化（APIエンドポイント）
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Recipe>> searchRecipes(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String ingredient,
            @RequestParam(required = false) Boolean favorite) {
        try {
            List<Recipe> allRecipes = recipeRepository.findAll();

            List<Recipe> filteredRecipes = allRecipes.stream()
                    .filter(recipe -> {
                        boolean matches = true;

                        if (title != null && !title.trim().isEmpty()) {
                            matches &= recipe.getTitle().toLowerCase()
                                    .contains(title.toLowerCase());
                        }

                        if (category != null && !category.trim().isEmpty()) {
                            matches &= recipe.getCategories().stream()
                                    .anyMatch(cat -> cat.toLowerCase()
                                            .contains(category.toLowerCase()));
                        }

                        if (ingredient != null && !ingredient.trim().isEmpty()) {
                            matches &= recipe.getIngredients().toLowerCase()
                                    .contains(ingredient.toLowerCase());
                        }

                        if (favorite != null) {
                            matches &= recipe.isFavorite() == favorite;
                        }

                        return matches;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredRecipes);
        } catch (Exception e) {
            System.err.println("検索エラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * レシピの一括削除（管理機能）- 全データリセット機能の実装
     */
    @PostMapping("/api/admin/reset-data")
    @ResponseBody
    public ResponseEntity<String> resetAllData() {
        try {
            System.out.println("全データリセット処理開始");

            // 画像ファイルも削除
            List<Recipe> allRecipes = recipeRepository.findAll();
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
            int deletedImageCount = 0;

            for (Recipe recipe : allRecipes) {
                if (recipe.getImagePath() != null) {
                    try {
                        Path imagePath = uploadPath.resolve(Paths.get(recipe.getImagePath()).getFileName());
                        if (Files.deleteIfExists(imagePath)) {
                            deletedImageCount++;
                            System.out.println("画像削除成功: " + imagePath.getFileName());
                        }
                    } catch (Exception imageDeleteError) {
                        System.err.println("画像削除エラー (継続): " + imageDeleteError.getMessage());
                    }
                }
            }

            // 全レシピをデータベースから削除
            int recipeCount = allRecipes.size();
            recipeRepository.deleteAll();

            System.out.println("全データリセット完了:");
            System.out.println("- 削除されたレシピ数: " + recipeCount);
            System.out.println("- 削除された画像数: " + deletedImageCount);

            return ResponseEntity.ok("データリセットが完了しました。削除されたレシピ: " + recipeCount + "件、画像: " + deletedImageCount + "件");

        } catch (Exception e) {
            System.err.println("データリセットエラー: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("データリセットに失敗しました: " + e.getMessage());
        }
    }

    // パラメータからレシピオブジェクトを作成するヘルパーメソッド
    private Recipe createRecipeFromParams(String title, String ingredients, String instructions, boolean favorite, String reference, Set<String> categories) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setIngredients(ingredients);
        recipe.setInstructions(instructions);
        recipe.setFavorite(favorite);
        recipe.setReference(reference);
        recipe.setCategories(categories);
        return recipe;
    }
}