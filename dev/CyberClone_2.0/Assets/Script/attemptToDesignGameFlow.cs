using System;
using System.Collections;
using UnityEngine;
using UnityEngine.SceneManagement;
public class attemptToDesignGameFLow:MonoBehaviour
{
    public static attemptToDesignGameFLow Instance {get;private set;}

    [Header("GAME FLOW | FADING")]
    public CanvasGroup fadeCanvasGroup; 
    public float fadeDuration = 1.5f;
    /// <summary>
    /// 
    /// 
    /// 
    /// 
    /// 
    /// </summary>
    [Header("GAME FLOW | SCENE NAMING")]
    public string mainMenuSc = "MainMenu";
    public string settingsSc = "Settings"; 
    public string creditSc = "Credits";
    private void Awake()
    {
    ///
    /// 
    /// i need to figure out how shoul d i start my project for this, i need some persistence
    /// in scene loads between, and to make sure that the gameobject doesnt get destroyed so some error handling
    /// 
    /// 
    if(Instance == null)
        {
            Instance = this; 
            DontDestroyOnLoad(gameObject); 

        }
        else
        {
            Destroy(gameObject);
        }
    }

public void LoadSceneInstant (string sceneName)
    {
        SceneManager.LoadScene(sceneName); 
    }

    public void LoadScene(string sceneName)
    {
        StartCoroutine(LoadSceneWithFade(sceneName));
    }

    public void BeginningGame()
    {
        SceneManager.LoadScene("Level1Intro"); //gonna changine this if i ever have an animated
        //flow of state
    }

    //need to set as well a level compeltion for when my character completes a level
    //think ill need to set that up in my build settings to put them in order 
    public void LoadingChronologicalScenes(String sceneTitle)
    {
        SceneManager.LoadScene(sceneTitle); 
    }


    // a resetter now if player dies, looping the entire process. either it can 
    //be changed to laod the very first level to annoy my playerr
    //or i can make em continue with the progress, so i might need to later call the points
    //my character gains from killing enemies here.

    public void restartingTitleScene()
    {
        Scene currentScene = SceneManager.GetActiveScene(); 
        SceneManager.LoadScene(currentScene.name);
    }
    
    //anddd nooowww return to main menu. or maybe another one where credits are due?

    public void returnanceToMenu()
    {
        SceneManager.LoadScene("MainMenu");
    }

    public void creditsActivaton()
    {
        SceneManager.LoadScene("Credits");
    }

    public void settingsPageMidGame()
    {
        SceneManager.LoadScene("Settings");
    }

    private IEnumerator LoadSceneWithFade(string sceneName)
    {
        if (fadeCanvasGroup!= null)
        {
            yield return StartCoroutine(FadeToAlpha(1.2f));

        }

        SceneManager.LoadScene(sceneName); 

        if (fadeCanvasGroup!=null)
        {
            yield return StartCoroutine(FadeToAlpha(0f));
        }
    }

    private IEnumerator LoadSceneWithFade(int sceneIndex)
    {
        if(fadeCanvasGroup!= null)
        {
            yield return StartCoroutine(FadeToAlpha(1f)); 

        }
        SceneManager.LoadScene(sceneIndex); 

        if(fadeCanvasGroup!=null)
        {
            yield return StartCoroutine(FadeToAlpha(0f));
        }
    }

    private IEnumerator FadeToAlpha (float targetAlpha)
    {
        float startAlpha = fadeCanvasGroup.alpha; 
        float elapsed = 0f; 
        while (elapsed <fadeDuration)
        {
            elapsed += Time.deltaTime;
            fadeCanvasGroup.alpha = Mathf.Lerp (startAlpha,targetAlpha,elapsed/ fadeDuration);
            yield return null;
        }
        fadeCanvasGroup.alpha = targetAlpha;
    }

    public void Quitter()
    {
//i really dont understand the logic behind how this quitter works so lets hope the unity 
//documentation is precies here.
        #if UNITY_EDITOR
        
            UnityEditor.EditorApplication.isPlaying = false;
        
        #else 
     Application.Quit();
        
        #endif 
    }
}