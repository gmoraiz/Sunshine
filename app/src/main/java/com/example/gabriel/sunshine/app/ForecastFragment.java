package com.example.gabriel.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Metodo que permite que o fragment segure um menu.
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu); //metodo que coloca o menu no fragment
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Metodo que irá tratar os clicks efetuados nos items do menu e apartir deles efetuar alguma ação
        int id = item.getItemId();
        if (id == R.id.action_reflesh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("940");
            return true;
        }
        return super.onOptionsItemSelected(item); // o retorno recursivo que será false caso não tenha clicado em nenhum item ou item inválido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Prepara uma lista de String para depois popular o adapter
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data)); //Cria a lista e coloca os dados

        //Aqui construimos um ArrayAdapter que será responsável pelos dados que irão popular a Lista visível ao usuário.
        // Ele fará a intervenção entre a Lista e o layout da lista
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // Captura o contexto da aplicação.
                        R.layout.list_item_forecast, // O nome do layout da lista
                        R.id.list_item_forecast_textview, // O Id do text view a ser populado.
                        weekForecast); // os dados que a popularão

        // Pegamos o layout do fragmento e colocamos em uma view para manipulação;
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Pega a referencia da listview criada no fragmento e coloca o adaptador, já preparado, lá dentro.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView; //retorna a View já preparada
    }

    // AsyncTask para criar uma tarefa em background para comunicação com o servidor das temperaturas.
    // Os três tipos genericos são, respectivamente: Os parametros, o progresso, o resultado
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        // A conversão de data/hora depois irá para fora do asynctask

        //Metodo que irá formatar a data.
        private String getReadableDateString(long time) {
            // a API retorna um timestamp, com isso precisamos formatar para uma data mais sucinta.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        //Este metodo formata as temperaturas minimas e maximas arredondando-as
        private String formatHighLows(double high, double low) {
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        // Neste metodo iremos formatar a String JSON recebida do servidor, para, a partir daí, extrair os valores necessários
        // E retornar uma nova string que será utilizada no adapter
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // Estes são os valores que iremos extrair do JSON.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr); // Criamos o  OBJETO JSON a partir da String que possui o JSON retornado do Servidor
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST); //Extraimos do objeto somente o array necessário para manipulação

            Time dayTime = new Time();
            dayTime.setToNow();

            // Pegamos o dia atual
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // Começamos a trabalhar com o UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays]; //Array de String com as informações de cada dia da semana

            for (int i = 0; i < weatherArray.length(); i++) {
                // Iremos usar o formato da string como:  "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Pegamos a parte do JSON Objeto que corresponde ao dia, conforme o loop
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime; // a data é retornada como long, mas iremos converter com aquele metodo lá em cima.

                //Aqui extraimos as informações do JSON

                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }

        //Este método ocorre em background para efetuar a conexão e receber seus dados
        @Override
        protected String[] doInBackground(String... params) {

            // Se não existir o código postal na entrada do parametro, retorno será null e nada será feito
            if (params.length == 0) {
                return null;
            }

            // Os dois são declarados fora do try catch
            HttpURLConnection urlConnection = null; // Conexão com o servidor
            BufferedReader reader = null; // efetua a leitura do InputStream que é o Response do Servidor

            String forecastJsonStr = null; // Irá obrigar o JSON retornado do servidor

            //Informações da URL
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construir a URL que será usada na requisição HTTP

                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString()); // instancia a nova URL

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // cria a requisição para o  OpenWeatherMap, e abre a conexão
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Lê as informações retornadas
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Lê as linhas do JSON
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        //Este método ocorre após a conexão assincrona terminar
        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for (String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}